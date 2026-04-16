package Entities.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void getUserIdentity_usesFallbackWhenEmpty() {
        User user = new User();

        user.setUsername(" ");
        user.setEmail(null);

        assertEquals("-\n-", user.getUserIdentity());
    }

    @Test
    void getUserIdentity_returnsUsernameAndEmail() {
        User user = new User();
        user.setUsername("abdallah");
        user.setEmail("abdallah@example.com");

        assertEquals("abdallah\nabdallah@example.com", user.getUserIdentity());
    }

    @Test
    void extractPrimaryRole_returnsDefaultWhenRolesMissing() {
        User user = new User();

        user.setRoles(null);
        assertEquals("ROLE_USER", user.extractPrimaryRole());

        user.setRoles(" ");
        assertEquals("ROLE_USER", user.extractPrimaryRole());
    }

    @Test
    void extractPrimaryRole_returnsFirstRoleFromJsonArray() {
        User user = new User();
        user.setRoles("[\"ROLE_ADMIN\",\"ROLE_USER\"]");

        assertEquals("ROLE_ADMIN", user.extractPrimaryRole());
    }

    @Test
    void getRank_removesRolePrefix() {
        User user = new User();
        user.setRoles("[\"ROLE_CREATOR\"]");

        assertEquals("CREATOR", user.getRank());
    }

    @Test
    void getStatus_prefersLockedOverActive() {
        User user = new User();
        user.setIsActive(1);
        user.setIsLocked(1);

        assertEquals("LOCKED", user.getStatus());
    }

    @Test
    void getStatus_activeAndInactiveCases() {
        User user = new User();

        user.setIsLocked(0);
        user.setIsActive(1);
        assertEquals("ACTIVE", user.getStatus());

        user.setIsActive(0);
        assertEquals("INACTIVE", user.getStatus());
    }

    @Test
    void isAdmin_trueWhenPrimaryRoleAdmin() {
        User user = new User();
        user.setRoles("[\"ROLE_ADMIN\"]");

        assertTrue(user.isAdmin());
    }

    @Test
    void isAdmin_trueWhenAdminExistsInRolesEvenIfNotPrimary() {
        User user = new User();
        user.setRoles("[\"ROLE_USER\",\"ROLE_ADMIN\"]");

        assertTrue(user.isAdmin());
    }

    @Test
    void isAdmin_falseWhenNoAdminRole() {
        User user = new User();
        user.setRoles("[\"ROLE_USER\"]");

        assertFalse(user.isAdmin());
    }
}
