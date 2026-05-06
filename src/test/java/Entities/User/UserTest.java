package Entities.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void identityFallsBackToPlaceholdersWhenFieldsAreBlank() {
        User user = new User();
        user.setUsername(" ");
        user.setEmail(null);

        assertEquals("-\n-", user.getUserIdentity());
    }

    @Test
    void avatarInitialsUseFirstTwoWordsWhenPresent() {
        User user = new User();
        user.setUsername("Shadow Walker");

        assertEquals("SW", user.getAvatarInitials());
    }

    @Test
    void primaryRoleStatusAndAdminFlagsReflectStoredRoles() {
        User user = new User();
        user.setRoles("[\"ROLE_ADMIN\", \"ROLE_USER\"]");
        user.setIsActive(1);
        user.setIsLocked(0);

        assertEquals("ROLE_ADMIN", user.extractPrimaryRole());
        assertEquals("ADMIN", user.getRank());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(user.isAdmin());
    }

    @Test
    void lockedStatusOverridesActiveFlag() {
        User user = new User();
        user.setRoles("[\"ROLE_USER\"]");
        user.setIsActive(1);
        user.setIsLocked(1);

        assertEquals("LOCKED", user.getStatus());
        assertFalse(user.isAdmin());
    }
}
