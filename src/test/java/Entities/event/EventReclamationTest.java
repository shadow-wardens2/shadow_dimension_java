package Entities.event;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class EventReclamationTest {

    @Test
    void testEventReclamationGettersAndSetters() {
        EventReclamation reclamation = new EventReclamation();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        reclamation.setId(1);
        reclamation.setUserId(2);
        reclamation.setEventId(3);
        reclamation.setStatus(EventReclamationStatus.OPEN);
        reclamation.setSubject("Issue");
        reclamation.setMessage("Message content");
        reclamation.setAiResponse("AI reply");
        reclamation.setAdminResponse("Admin reply");
        reclamation.setCreatedAt(now);
        reclamation.setUpdatedAt(now);

        // Virtual fields
        reclamation.setUsername("testuser");
        reclamation.setUserEmail("test@test.com");
        reclamation.setEventTitle("Test Event");

        assertEquals(1, reclamation.getId());
        assertEquals(2, reclamation.getUserId());
        assertEquals(3, reclamation.getEventId());
        assertEquals(EventReclamationStatus.OPEN, reclamation.getStatus());
        assertEquals("Issue", reclamation.getSubject());
        assertEquals("Message content", reclamation.getMessage());
        assertEquals("AI reply", reclamation.getAiResponse());
        assertEquals("Admin reply", reclamation.getAdminResponse());
        assertEquals(now, reclamation.getCreatedAt());
        assertEquals(now, reclamation.getUpdatedAt());

        assertEquals("testuser", reclamation.getUsername());
        assertEquals("test@test.com", reclamation.getUserEmail());
        assertEquals("Test Event", reclamation.getEventTitle());
    }

    @Test
    void testGetStatusLabel() {
        EventReclamation reclamation = new EventReclamation();
        
        // When status is null
        assertEquals("OPEN", reclamation.getStatusLabel());

        // When status is set
        reclamation.setStatus(EventReclamationStatus.IN_PROGRESS);
        assertEquals("IN_PROGRESS", reclamation.getStatusLabel());
    }

    @Test
    void testCanEscalate() {
        EventReclamation reclamation = new EventReclamation();
        
        reclamation.setStatus(EventReclamationStatus.OPEN);
        assertTrue(reclamation.canEscalate());

        reclamation.setStatus(EventReclamationStatus.IN_PROGRESS);
        assertTrue(reclamation.canEscalate());

        reclamation.setStatus(EventReclamationStatus.AI_RESPONDED);
        assertTrue(reclamation.canEscalate());

        reclamation.setStatus(EventReclamationStatus.RESOLVED);
        assertFalse(reclamation.canEscalate());

        reclamation.setStatus(EventReclamationStatus.CLOSED);
        assertFalse(reclamation.canEscalate());
    }

    @Test
    void testConstructor() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        EventReclamation reclamation = new EventReclamation(
                10, 20, 30, EventReclamationStatus.RESOLVED, "Sub", "Msg", "AI", "Admin", now, now
        );

        assertEquals(10, reclamation.getId());
        assertEquals(20, reclamation.getUserId());
        assertEquals(30, reclamation.getEventId());
        assertEquals(EventReclamationStatus.RESOLVED, reclamation.getStatus());
        assertEquals("Sub", reclamation.getSubject());
        assertEquals("Msg", reclamation.getMessage());
        assertEquals("AI", reclamation.getAiResponse());
        assertEquals("Admin", reclamation.getAdminResponse());
    }
}
