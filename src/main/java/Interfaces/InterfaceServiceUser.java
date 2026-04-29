package Interfaces;

import Entities.User.User;

import java.sql.SQLException;
import java.util.List;

public interface InterfaceServiceUser {
    // Local authentication.
    User signup(String email, String username, String plainPassword) throws SQLException;

    User login(String emailOrUsername, String plainPassword) throws SQLException;

    // Google authentication/linking.
    User loginOrSignupWithGoogle(String googleId, String email, String fullName) throws SQLException;

    // Email verification flow.
    void resendVerificationCode(String email) throws SQLException;

    boolean verifyEmailCode(String email, String code) throws SQLException;

    // Password reset flow.
    void sendPasswordResetCode(String emailOrUsername) throws SQLException;

    boolean resetPassword(String email, String code, String newPassword) throws SQLException;

    // Face ID prototype flow.
    boolean isFaceIdEnabled(int userId) throws SQLException;

    void enrollFaceId(int userId, String faceSignature) throws SQLException;

    void disableFaceId(int userId) throws SQLException;

    User loginWithFace(String faceSignature) throws SQLException;

    // Profile and admin management.
    User getByIdentity(String emailOrUsername) throws SQLException;

    User getById(int id) throws SQLException;

    void updateProfile(User user) throws SQLException;

    List<User> getAllUsers() throws SQLException;

    void updateUserByAdmin(User user) throws SQLException;

    void deleteUserById(int userId) throws SQLException;
}
