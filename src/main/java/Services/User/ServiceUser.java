package Services.User;

import Entities.User.User;
import Interfaces.InterfaceServiceUser;
import Utils.ShadowDimensionsDB;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // DB connection and auxiliary email service.
    private final Connection cnx;
    private final EmailService emailService = new EmailService();

    public ServiceUser() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
        ensureVerificationTable();
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

        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified FROM `user` WHERE email = ? OR username = ?";
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

        int isVerified = rs.getInt("is_verified");
        if (isVerified == 0) {
            throw new IllegalArgumentException("Email non verifie. Entrez le code recu par mail.");
        }

        String storedPassword = rs.getString("password");
        if (!isPasswordValid(plainPassword, storedPassword)) {
            return null;
        }

        return mapUser(rs);
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

    // Profile and admin CRUD operations.
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

    public void deleteUserById(int userId) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
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
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(10));

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

        if (sb.length() > 0) {
            throw new IllegalArgumentException("Le mot de passe doit respecter:\n" + sb);
        }
    }

    private boolean isValidTunisiaPhone(String phone) {
        return phone.matches("^\\+216\\d{8}$");
    }
}
