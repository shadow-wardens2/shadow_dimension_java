package Interfaces;

import Entities.User.User;

import java.sql.SQLException;
import java.util.List;

public interface InterfaceServiceUser {
    User signup(String email, String username, String plainPassword) throws SQLException;

    User login(String emailOrUsername, String plainPassword) throws SQLException;

    User loginOrSignupWithGoogle(String googleId, String email, String fullName) throws SQLException;

    void resendVerificationCode(String email) throws SQLException;

    boolean verifyEmailCode(String email, String code) throws SQLException;

    User getById(int id) throws SQLException;

    void updateProfile(User user) throws SQLException;

    List<User> getAllUsers() throws SQLException;

    void updateUserByAdmin(User user) throws SQLException;

    void deleteUserById(int userId) throws SQLException;
}
