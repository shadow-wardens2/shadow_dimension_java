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
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements InterfaceServiceUser {

    private final Connection cnx;

    public ServiceUser() {
        cnx = ShadowDimensionsDB.getInstance().getConnection();
    }

    public User signup(String email, String username, String plainPassword) throws SQLException {
        if (email == null || email.isBlank() || username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

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
        ps.setInt(6, 1);
        ps.setInt(7, 0);
        ps.setInt(8, 0);
        ps.setInt(9, 0);
        ps.setInt(10, 0);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            return getById(keys.getInt(1));
        }

        return findByEmail(email.trim());
    }

    public User login(String emailOrUsername, String plainPassword) throws SQLException {
        if (emailOrUsername == null || emailOrUsername.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Email/Username et mot de passe sont obligatoires.");
        }

        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, is_active, is_locked FROM `user` WHERE email = ? OR username = ?";
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
            return null;
        }

        return mapUser(rs);
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, is_active, is_locked FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapUser(rs);
        }

        return null;
    }

    public void updateProfile(User user) throws SQLException {
        String sql = "UPDATE `user` SET email = ?, username = ?, full_name = ?, phone = ?, country = ?, city = ?, bio = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getFullName());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getCountry());
        ps.setString(6, user.getCity());
        ps.setString(7, user.getBio());
        ps.setInt(8, user.getId());
        ps.executeUpdate();
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, is_active, is_locked FROM `user` ORDER BY id DESC";
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

    private User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, is_active, is_locked FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

    private User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, email, username, roles, password, full_name, phone, country, city, bio, is_active, is_locked FROM `user` WHERE username = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapUser(rs);
        }
        return null;
    }

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
        user.setIsActive(rs.getInt("is_active"));
        user.setIsLocked(rs.getInt("is_locked"));
        return user;
    }

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
}
