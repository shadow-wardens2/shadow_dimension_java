package Services.User;

import Entities.User.User;
import Interfaces.InterfaceServiceUser;
import Utils.OpenCvFaceAuthUtil;
import Utils.ShadowDimensionsDB;
import org.mindrot.jbcrypt.BCrypt;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class ServiceUser implements InterfaceServiceUser {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 3;
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{};:,.?/|";
    private static final String ALL_PASSWORD_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    private static final int SUGGESTED_PASSWORD_LENGTH = 12;

    // DB connection and auxiliary email service.
    private final Connection cnx;
    private final EmailService emailService = new EmailService();
    private final SecureRandom secureRandom = new SecureRandom();

    public ServiceUser() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        ensureUserColumns();
        ensureVerificationTable();
        ensurePasswordResetTable();
        ensureFaceIdTable();
    }

    // Local signup (email/password) with password policy + verification email.
    public User signup(String email, String username, String plainPassword) throws SQLException {
        if (email == null || email.isBlank() || username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        validatePasswordPolicy(plainPassword);

        if (findByEmail(email) != null) {
            throw new IllegalArgumentException("Cet email existe deja.");
        }

        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("Ce username existe deja.");
        }

        String sql = "INSERT INTO `user` (email, username, roles, password, is_active, is_verified, created_at, accessibility_mode, onboarding_completed, bad_comment_count, is_locked) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, email.trim());
        ps.setString(2, username.trim());
        ps.setString(3, "[\"ROLE_USER\"]");
        ps.setString(4, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        ps.setInt(5, 1);
        ps.setInt(6, 0);
        ps.setInt(7, 0);
        ps.setInt(8, 0);
        ps.setInt(9, 0);
        ps.setInt(10, 0);
        ps.executeUpdate();

        User createdUser;
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            createdUser = getById(keys.getInt(1));
        } else {
            createdUser = findByEmail(email.trim());
        }

        if (createdUser == null) {
            throw new IllegalStateException("Impossible de recuperer l'utilisateur cree.");
        }

        try {
            sendVerificationCode(createdUser);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Compte cree mais email de verification non envoye. Verifiez MAIL_* et SMTP, puis utilisez Resend Code. Détail: " + ex.getMessage(),
                    ex
            );
        }

        return createdUser;
    }

    // Local login with lock/verification checks and password validation.
    public User login(String emailOrUsername, String plainPassword) throws SQLException {
        if (emailOrUsername == null || emailOrUsername.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Email/Username et mot de passe sont obligatoires.");
        }

        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified, failed_login_attempts FROM `user` WHERE email = ? OR username = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, emailOrUsername.trim());
        ps.setString(2, emailOrUsername.trim());
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            return null;
        }

        int isLocked = rs.getInt("is_locked");
        if (isLocked == 1) {
            throw new IllegalArgumentException("Ce compte est verrouille.");
        }

        String storedPassword = rs.getString("password");
        if (!isPasswordValid(plainPassword, storedPassword)) {
            int failedAttempts = rs.getInt("failed_login_attempts") + 1;
            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                lockUserAfterFailedAttempts(rs.getInt("id"), failedAttempts);
                throw new IllegalArgumentException("Compte bloque apres 3 tentatives echouees. Seul un admin peut le debloquer.");
            }

            updateFailedLoginAttempts(rs.getInt("id"), failedAttempts);
            int remainingAttempts = MAX_FAILED_LOGIN_ATTEMPTS - failedAttempts;
            throw new IllegalArgumentException("Mot de passe incorrect. Il reste " + remainingAttempts + " tentative(s) avant blocage.");
        }

        if (rs.getInt("is_verified") == 0) {
            markUserAsVerified(rs.getInt("id"));
        }

        resetFailedLoginAttempts(rs.getInt("id"));
        return getById(rs.getInt("id"));
    }

    // Google auth flow: login existing, link by email, or create a new user.
    public User loginOrSignupWithGoogle(String googleId, String email, String fullName) throws SQLException {
        if (googleId == null || googleId.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google id et email sont obligatoires.");
        }

        User byGoogle = findByGoogleId(googleId.trim());
        if (byGoogle != null) {
            if (byGoogle.getIsLocked() == 1) {
                throw new IllegalArgumentException("Ce compte est verrouille.");
            }
            return byGoogle;
        }

        User byEmail = findByEmail(email.trim());
        if (byEmail != null) {
            if (byEmail.getIsLocked() == 1) {
                throw new IllegalArgumentException("Ce compte est verrouille.");
            }

            String updateSql = "UPDATE `user` SET google_id = ?, full_name = COALESCE(NULLIF(full_name, ''), ?) WHERE id = ?";
            PreparedStatement updatePs = cnx.prepareStatement(updateSql);
            updatePs.setString(1, googleId.trim());
            updatePs.setString(2, fullName == null ? "" : fullName.trim());
            updatePs.setInt(3, byEmail.getId());
            updatePs.executeUpdate();

            return getById(byEmail.getId());
        }

        String username = generateUniqueUsernameFromEmail(email);
        String sql = "INSERT INTO `user` (email, username, roles, password, google_id, full_name, is_active, is_verified, created_at, accessibility_mode, onboarding_completed, bad_comment_count, is_locked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, email.trim());
        ps.setString(2, username);
        ps.setString(3, "[\"ROLE_USER\"]");
        ps.setString(4, BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
        ps.setString(5, googleId.trim());
        ps.setString(6, fullName == null ? "" : fullName.trim());
        ps.setInt(7, 1);
        ps.setInt(8, 1);
        ps.setInt(9, 0);
        ps.setInt(10, 0);
        ps.setInt(11, 0);
        ps.setInt(12, 0);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            return getById(keys.getInt(1));
        }

        return findByEmail(email.trim());
    }

    // Verification-code API.
    public void resendVerificationCode(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email obligatoire.");
        }

        User user = findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouve pour cet email.");
        }

        if (user.getIsVerified() == 1) {
            throw new IllegalArgumentException("Cet email est deja verifie.");
        }

        sendVerificationCode(user);
    }

    public boolean verifyEmailCode(String email, String code) throws SQLException {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            throw new IllegalArgumentException("Email et code sont obligatoires.");
        }

        User user = findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouve pour cet email.");
        }

        String sql = "SELECT id, expires_at, consumed FROM user_verification_codes WHERE user_id = ? AND code = ? ORDER BY id DESC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, user.getId());
        ps.setString(2, code.trim());
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            return false;
        }

        if (rs.getInt("consumed") == 1) {
            return false;
        }

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt == null || expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        int verificationId = rs.getInt("id");

        PreparedStatement consumePs = cnx.prepareStatement("UPDATE user_verification_codes SET consumed = 1 WHERE id = ?");
        consumePs.setInt(1, verificationId);
        consumePs.executeUpdate();

        PreparedStatement verifyUserPs = cnx.prepareStatement("UPDATE `user` SET is_verified = 1 WHERE id = ?");
        verifyUserPs.setInt(1, user.getId());
        verifyUserPs.executeUpdate();

        return true;
    }

    public void sendPasswordResetCode(String emailOrUsername) throws SQLException {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new IllegalArgumentException("Email ou username obligatoire.");
        }

        User user = findByIdentity(emailOrUsername.trim());
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouve pour cet identifiant.");
        }

        if (user.getIsLocked() == 1) {
            throw new IllegalArgumentException("Ce compte est verrouille.");
        }

        persistAndSendPasswordResetCode(user);
    }

    public boolean resetPassword(String email, String code, String newPassword) throws SQLException {
        if (email == null || email.isBlank() || code == null || code.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Email, code et nouveau mot de passe sont obligatoires.");
        }

        validatePasswordPolicy(newPassword);

        User user = findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouve pour cet email.");
        }

        String sql = "SELECT id, expires_at, consumed FROM user_password_reset_codes WHERE user_id = ? AND code = ? ORDER BY id DESC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, user.getId());
        ps.setString(2, code.trim());
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            return false;
        }

        if (rs.getInt("consumed") == 1) {
            return false;
        }

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt == null || expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        int resetId = rs.getInt("id");

        PreparedStatement updatePasswordPs = cnx.prepareStatement("UPDATE `user` SET password = ? WHERE id = ?");
        updatePasswordPs.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        updatePasswordPs.setInt(2, user.getId());
        updatePasswordPs.executeUpdate();

        PreparedStatement consumePs = cnx.prepareStatement("UPDATE user_password_reset_codes SET consumed = 1 WHERE id = ?");
        consumePs.setInt(1, resetId);
        consumePs.executeUpdate();

        PreparedStatement invalidatePs = cnx.prepareStatement("UPDATE user_password_reset_codes SET consumed = 1 WHERE user_id = ? AND consumed = 0");
        invalidatePs.setInt(1, user.getId());
        invalidatePs.executeUpdate();

        return true;
    }

    public String generateSuggestedPassword() {
        StringBuilder password = new StringBuilder(SUGGESTED_PASSWORD_LENGTH);
        password.append(randomChar(UPPERCASE_CHARS));
        password.append(randomChar(LOWERCASE_CHARS));
        password.append(randomChar(DIGIT_CHARS));
        password.append(randomChar(SPECIAL_CHARS));

        while (password.length() < SUGGESTED_PASSWORD_LENGTH) {
            password.append(randomChar(ALL_PASSWORD_CHARS));
        }

        shuffle(password);
        return password.toString();
    }

    public boolean isFaceIdEnabled(int userId) throws SQLException {
        String sql = "SELECT enabled FROM user_face_auth WHERE user_id = ? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt("enabled") == 1;
    }

    public void enrollFaceId(int userId, String faceSignature) throws SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }
        if (faceSignature == null || faceSignature.isBlank()) {
            throw new IllegalArgumentException("Signature faciale invalide.");
        }

        PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO user_face_auth (user_id, face_signature, enabled, updated_at) VALUES (?, ?, 1, NOW()) " +
                        "ON DUPLICATE KEY UPDATE face_signature = VALUES(face_signature), enabled = 1, updated_at = NOW()"
        );
        ps.setInt(1, userId);
        ps.setString(2, faceSignature);
        ps.executeUpdate();
    }

    public void disableFaceId(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE user_face_auth SET enabled = 0, updated_at = NOW() WHERE user_id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public User loginWithFace(String faceSignature) throws SQLException {
        if (faceSignature == null || faceSignature.isBlank()) {
            throw new IllegalArgumentException("Capture faciale invalide.");
        }

        String sql = "SELECT u.id, u.email, u.username, u.roles, u.password, u.full_name, u.phone, u.country, u.city, u.bio, u.created_at, u.is_active, u.is_locked, u.is_verified, ufa.face_signature " +
                "FROM user_face_auth ufa " +
                "JOIN `user` u ON u.id = ufa.user_id " +
                "WHERE ufa.enabled = 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String storedSignature = rs.getString("face_signature");
            if (!OpenCvFaceAuthUtil.matches(storedSignature, faceSignature)) {
                continue;
            }

            if (rs.getInt("is_locked") == 1) {
                throw new IllegalArgumentException("Ce compte est verrouille.");
            }

            if (rs.getInt("is_verified") == 0) {
                throw new IllegalArgumentException("Email non verifie. Entrez le code recu par mail.");
            }

            return mapUser(rs);
        }

        return null;
    }

    public User loginWithFaceImage(BufferedImage image) throws SQLException {
        FaceLoginAttempt attempt = analyzeFaceLogin(image);
        return attempt.matchedUser();
    }

    public FaceLoginAttempt analyzeFaceLogin(BufferedImage image) throws SQLException {
        if (image == null) {
            throw new IllegalArgumentException("Capture faciale invalide.");
        }

        OpenCvFaceAuthUtil.FaceAnalysis analysis = OpenCvFaceAuthUtil.analyzeFace(image);
        if (!analysis.faceDetected()) {
            return FaceLoginAttempt.noFace();
        }

        String sql = "SELECT u.id, u.email, u.username, u.roles, u.password, u.full_name, u.phone, u.country, u.city, u.bio, u.created_at, u.is_active, u.is_locked, u.is_verified, ufa.face_signature " +
                "FROM user_face_auth ufa " +
                "JOIN `user` u ON u.id = ufa.user_id " +
                "WHERE ufa.enabled = 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        User bestUser = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        while (rs.next()) {
            String storedSignature = rs.getString("face_signature");
            OpenCvFaceAuthUtil.MatchResult match = OpenCvFaceAuthUtil.matchStoredSignature(storedSignature, analysis, image);
            if (!match.matched() || match.score() <= bestScore) {
                continue;
            }

            bestScore = match.score();
            bestUser = mapUser(rs);
        }

        if (bestUser == null) {
            return FaceLoginAttempt.detected(analysis.faceBounds());
        }

        if (bestUser.getIsLocked() == 1) {
            throw new IllegalArgumentException("Ce compte est verrouille.");
        }

        if (bestUser.getIsVerified() == 0) {
            throw new IllegalArgumentException("Email non verifie. Entrez le code recu par mail.");
        }

        return FaceLoginAttempt.matched(bestUser, analysis.faceBounds(), bestScore);
    }

    public String buildFaceSignature(BufferedImage image) {
        return OpenCvFaceAuthUtil.buildFaceSignature(image);
    }

    public String buildFaceSignature(List<BufferedImage> images) {
        return OpenCvFaceAuthUtil.buildFaceSignature(images);
    }

    public record FaceLoginAttempt(User matchedUser, Rectangle faceBounds, boolean faceDetected, double similarityScore) {
        public static FaceLoginAttempt noFace() {
            return new FaceLoginAttempt(null, null, false, Double.NEGATIVE_INFINITY);
        }

        public static FaceLoginAttempt detected(Rectangle faceBounds) {
            return new FaceLoginAttempt(null, faceBounds, true, Double.NEGATIVE_INFINITY);
        }

        public static FaceLoginAttempt matched(User user, Rectangle faceBounds, double similarityScore) {
            return new FaceLoginAttempt(user, faceBounds, true, similarityScore);
        }
    }

    public String getPasswordPolicyMessage(String password) {
        if (password == null || password.isBlank()) {
            return "Le mot de passe est obligatoire.";
        }

        StringBuilder sb = new StringBuilder();

        if (password.length() < 8) {
            sb.append("- 8 caracteres minimum\n");
        }
        if (!password.matches(".*[A-Z].*")) {
            sb.append("- Au moins une lettre majuscule\n");
        }
        if (!password.matches(".*[a-z].*")) {
            sb.append("- Au moins une lettre minuscule\n");
        }
        if (!password.matches(".*\\d.*")) {
            sb.append("- Au moins un chiffre\n");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            sb.append("- Au moins un caractere special\n");
        }

        if (sb.length() == 0) {
            return "";
        }

        return "Le mot de passe doit respecter:\n" + sb;
    }

    // Profile and admin CRUD operations.
    public User getByIdentity(String emailOrUsername) throws SQLException {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            return null;
        }
        return findByIdentity(emailOrUsername.trim());
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapUser(rs);
        }

        return null;
    }

    public void updateProfile(User user) throws SQLException {
        String phone = user.getPhone() == null ? "" : user.getPhone().trim();
        if (!phone.isBlank() && !isValidTunisiaPhone(phone)) {
            throw new IllegalArgumentException("Le numero de telephone doit etre au format +216 suivi de 8 chiffres.");
        }

        String sql = "UPDATE `user` SET email = ?, username = ?, full_name = ?, phone = ?, country = ?, city = ?, bio = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getFullName());
        ps.setString(4, phone);
        ps.setString(5, user.getCountry());
        ps.setString(6, user.getCity());
        ps.setString(7, user.getBio());
        ps.setInt(8, user.getId());
        ps.executeUpdate();
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        List<User> users = new ArrayList<>();

        while (rs.next()) {
            users.add(mapUser(rs));
        }

        return users;
    }

    public void updateUserByAdmin(User user) throws SQLException {
        String sql = "UPDATE `user` SET email = ?, username = ?, roles = ?, full_name = ?, phone = ?, country = ?, city = ?, bio = ?, is_active = ?, is_locked = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getRoles());
        ps.setString(4, user.getFullName());
        ps.setString(5, user.getPhone());
        ps.setString(6, user.getCountry());
        ps.setString(7, user.getCity());
        ps.setString(8, user.getBio());
        ps.setInt(9, user.getIsActive());
        ps.setInt(10, user.getIsLocked());
        ps.setInt(11, user.getId());
        ps.executeUpdate();
    }

    public void resetFailedLoginAttempts(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE `user` SET failed_login_attempts = 0 WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public void deleteUserById(int userId) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public User getFirstActiveAdmin() throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE roles LIKE '%ROLE_ADMIN%' AND is_active = 1 LIMIT 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    // Internal lookup helpers.
    private User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    private User findByIdentity(String emailOrUsername) throws SQLException {
        User byEmail = findByEmail(emailOrUsername);
        if (byEmail != null) {
            return byEmail;
        }
        return findByUsername(emailOrUsername);
    }

    private User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE username = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    private User findByGoogleId(String googleId) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE google_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, googleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    private String generateUniqueUsernameFromEmail(String email) throws SQLException {
        String localPart = email.split("@", 2)[0].toLowerCase(Locale.ROOT);
        String base = localPart.replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "soul";
        }

        String candidate = base;
        int suffix = 1;
        while (findByUsername(candidate) != null) {
            candidate = base + suffix;
            suffix++;
        }

        return candidate;
    }

    // ResultSet -> User mapper.
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setRoles(rs.getString("roles"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setCountry(rs.getString("country"));
        user.setCity(rs.getString("city"));
        user.setBio(rs.getString("bio"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setLastPresence(createdAt.toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        } else {
            user.setLastPresence("-");
        }
        user.setIsActive(rs.getInt("is_active"));
        user.setIsLocked(rs.getInt("is_locked"));
        user.setIsVerified(rs.getInt("is_verified"));
        return user;
    }

    // Verification-code persistence + outbound email send.
    private void sendVerificationCode(User user) throws SQLException {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(3));

        PreparedStatement invalidatePs = cnx.prepareStatement("UPDATE user_verification_codes SET consumed = 1 WHERE user_id = ? AND consumed = 0");
        invalidatePs.setInt(1, user.getId());
        invalidatePs.executeUpdate();

        PreparedStatement insertPs = cnx.prepareStatement("INSERT INTO user_verification_codes (user_id, code, expires_at, consumed) VALUES (?, ?, ?, 0)");
        insertPs.setInt(1, user.getId());
        insertPs.setString(2, code);
        insertPs.setTimestamp(3, expiresAt);
        insertPs.executeUpdate();

        emailService.sendVerificationCode(user.getEmail(), user.getUsername(), code);
    }

    private void persistAndSendPasswordResetCode(User user) throws SQLException {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(3));

        PreparedStatement invalidatePs = cnx.prepareStatement("UPDATE user_password_reset_codes SET consumed = 1 WHERE user_id = ? AND consumed = 0");
        invalidatePs.setInt(1, user.getId());
        invalidatePs.executeUpdate();

        PreparedStatement insertPs = cnx.prepareStatement("INSERT INTO user_password_reset_codes (user_id, code, expires_at, consumed) VALUES (?, ?, ?, 0)");
        insertPs.setInt(1, user.getId());
        insertPs.setString(2, code);
        insertPs.setTimestamp(3, expiresAt);
        insertPs.executeUpdate();

        emailService.sendPasswordResetCode(user.getEmail(), user.getUsername(), code);
    }

    // Ensures verification-code table exists at service startup.
    private void ensureVerificationTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_verification_codes ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "code VARCHAR(10) NOT NULL, "
                + "expires_at DATETIME NOT NULL, "
                + "consumed TINYINT(1) NOT NULL DEFAULT 0, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_user_verif_user_id (user_id), "
                + "CONSTRAINT fk_user_verif_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Keep startup resilient; verification methods throw clear errors if table access fails.
        }
    }

    // Password helpers (legacy compatibility + policy).
    private String hashPassword(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private boolean isPasswordValid(String plainPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // jBCrypt expects $2a$/$2b$. Legacy PHP exports often use $2y$.
            String normalized = storedPassword.startsWith("$2y$")
                    ? "$2a$" + storedPassword.substring(4)
                    : storedPassword;
            try {
                return BCrypt.checkpw(plainPassword, normalized);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        // Compatibility fallback for previously inserted SHA-256 passwords.
        return storedPassword.equals(hashPassword(plainPassword));
    }

    private void validatePasswordPolicy(String password) {
        String policyMessage = getPasswordPolicyMessage(password);
        if (!policyMessage.isEmpty()) {
            throw new IllegalArgumentException(policyMessage);
        }
    }

    private void ensurePasswordResetTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_password_reset_codes ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "code VARCHAR(10) NOT NULL, "
                + "expires_at DATETIME NOT NULL, "
                + "consumed TINYINT(1) NOT NULL DEFAULT 0, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_user_reset_user_id (user_id), "
                + "CONSTRAINT fk_user_reset_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Keep startup resilient; reset methods throw clear errors if table access fails.
        }
    }

    private void ensureUserColumns() {
        String[] columns = {
            "ALTER TABLE `user` ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0",
            "ALTER TABLE `user` ADD COLUMN IF NOT EXISTS is_verified TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE `user` ADD COLUMN IF NOT EXISTS is_locked TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE `user` ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) DEFAULT NULL"
        };
        for (String sql : columns) {
            try (Statement st = cnx.createStatement()) {
                st.execute(sql);
            } catch (SQLException ignored) {
            }
        }
    }

    private void ensureFaceIdTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_face_auth ("
                + "user_id INT NOT NULL PRIMARY KEY, "
                + "face_signature LONGTEXT NOT NULL, "
                + "enabled TINYINT(1) NOT NULL DEFAULT 1, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_user_face_auth_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Keep startup resilient; face-id methods throw clearer errors if table access fails.
        }
    }

    private boolean isValidTunisiaPhone(String phone) {
        return phone.matches("^\\+216\\d{8}$");
    }

    private char randomChar(String candidateChars) {
        return candidateChars.charAt(secureRandom.nextInt(candidateChars.length()));
    }

    private void shuffle(StringBuilder value) {
        for (int i = value.length() - 1; i > 0; i--) {
            int swapIndex = secureRandom.nextInt(i + 1);
            char current = value.charAt(i);
            value.setCharAt(i, value.charAt(swapIndex));
            value.setCharAt(swapIndex, current);
        }
    }

    private void updateFailedLoginAttempts(int userId, int failedAttempts) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE `user` SET failed_login_attempts = ? WHERE id = ?");
        ps.setInt(1, failedAttempts);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    private void lockUserAfterFailedAttempts(int userId, int failedAttempts) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE `user` SET failed_login_attempts = ?, is_locked = 1 WHERE id = ?");
        ps.setInt(1, failedAttempts);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    private void markUserAsVerified(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE `user` SET is_verified = 1 WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }
}
