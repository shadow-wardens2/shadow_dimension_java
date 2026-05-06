package Services.User;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceUserTest {

    @Test
    void passwordPolicyMessageListsAllMissingRequirements() {
        ServiceUser service = new ServiceUser(null, null, new SecureRandom(), false);

        String message = service.getPasswordPolicyMessage("short");

        assertEquals("""
                Le mot de passe doit respecter:
                - 8 caracteres minimum
                - Au moins une lettre majuscule
                - Au moins un chiffre
                - Au moins un caractere special
                """, message);
    }

    @Test
    void generatedPasswordMatchesExpectedPolicy() {
        ServiceUser service = new ServiceUser(null, null, new SecureRandom(), false);

        String password = service.generateSuggestedPassword();

        assertNotNull(password);
        assertEquals(12, password.length());
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch)));
        assertTrue(service.getPasswordPolicyMessage(password).isEmpty());
    }

    @Test
    void updateProfileRejectsInvalidTunisiaPhoneBeforeDatabaseWrite() throws Exception {
        Connection connection = mock(Connection.class);
        ServiceUser service = new ServiceUser(connection, null, new SecureRandom(), false);

        Entities.User.User user = new Entities.User.User();
        user.setPhone("12345");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.updateProfile(user));

        assertEquals("Le numero de telephone doit etre au format +216 suivi de 8 chiffres.", exception.getMessage());
        verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void loginWithWrongPasswordTracksRemainingAttempts() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lookupStatement = mock(PreparedStatement.class);
        PreparedStatement updateStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(eq("SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified, failed_login_attempts FROM `user` WHERE email = ? OR username = ?"))).thenReturn(lookupStatement);
        when(connection.prepareStatement(eq("UPDATE `user` SET failed_login_attempts = ? WHERE id = ?"))).thenReturn(updateStatement);
        when(lookupStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(7);
        when(resultSet.getInt("is_locked")).thenReturn(0);
        when(resultSet.getInt("failed_login_attempts")).thenReturn(1);
        when(resultSet.getString("password")).thenReturn("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiZ7n6xP3YfJmRoTSesFiNUFDXL9O5K");

        ServiceUser service = new ServiceUser(connection, null, new SecureRandom(), false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.login("mage@shadow.com", "WrongPassword1!"));

        assertEquals("Mot de passe incorrect. Il reste 1 tentative(s) avant blocage.", exception.getMessage());
        verify(updateStatement).setInt(1, 2);
        verify(updateStatement).setInt(2, 7);
        verify(updateStatement).executeUpdate();
    }

    @Test
    void loginLocksUserOnThirdFailedAttempt() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lookupStatement = mock(PreparedStatement.class);
        PreparedStatement lockStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(eq("SELECT id, email, username, roles, password, full_name, phone, country, city, bio, created_at, is_active, is_locked, is_verified, failed_login_attempts FROM `user` WHERE email = ? OR username = ?"))).thenReturn(lookupStatement);
        when(connection.prepareStatement(eq("UPDATE `user` SET failed_login_attempts = ?, is_locked = 1 WHERE id = ?"))).thenReturn(lockStatement);
        when(lookupStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(11);
        when(resultSet.getInt("is_locked")).thenReturn(0);
        when(resultSet.getInt("failed_login_attempts")).thenReturn(2);
        when(resultSet.getString("password")).thenReturn("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiZ7n6xP3YfJmRoTSesFiNUFDXL9O5K");

        ServiceUser service = new ServiceUser(connection, null, new SecureRandom(), false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.login("locked@shadow.com", "WrongPassword1!"));

        assertEquals("Compte bloque apres 3 tentatives echouees. Seul un admin peut le debloquer.", exception.getMessage());
        verify(lockStatement).setInt(1, 3);
        verify(lockStatement).setInt(2, 11);
        verify(lockStatement).executeUpdate();
    }

    @Test
    void resetPasswordRejectsWeakPasswordBeforeDatabaseLookup() throws Exception {
        Connection connection = mock(Connection.class);
        ServiceUser service = new ServiceUser(connection, null, new SecureRandom(), false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("mage@shadow.com", "123456", "weak"));

        assertTrue(exception.getMessage().contains("- 8 caracteres minimum"));
        verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void blankIdentityLookupReturnsNull() throws Exception {
        ServiceUser service = new ServiceUser(null, null, new SecureRandom(), false);

        assertNull(service.getByIdentity("   "));
    }
}
