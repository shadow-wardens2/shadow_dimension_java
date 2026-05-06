package Entities.event;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testReservationGettersAndSetters() {
        Reservation reservation = new Reservation();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        reservation.setId(1);
        reservation.setUserId(10);
        reservation.setEventId(20);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedAt(now);
        reservation.setQrCodeChecked(true);

        // Virtual properties
        reservation.setUsername("testuser");
        reservation.setUserEmail("test@test.com");
        reservation.setUserPhone("12345678");
        reservation.setEventTitle("Test Event");
        reservation.setEventStartDate(now);
        reservation.setEventEndDate(now);

        assertEquals(1, reservation.getId());
        assertEquals(10, reservation.getUserId());
        assertEquals(20, reservation.getEventId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(now, reservation.getReservedAt());
        assertTrue(reservation.isQrCodeChecked());

        assertEquals("testuser", reservation.getUsername());
        assertEquals("test@test.com", reservation.getUserEmail());
        assertEquals("12345678", reservation.getUserPhone());
        assertEquals("Test Event", reservation.getEventTitle());
        assertEquals(now, reservation.getEventStartDate());
        assertEquals(now, reservation.getEventEndDate());
    }

    @Test
    void testGetStatusLabel() {
        Reservation reservation = new Reservation();
        
        // Null status
        assertEquals("UNKNOWN", reservation.getStatusLabel());

        // Valid status
        reservation.setStatus(ReservationStatus.ACCEPTED);
        assertEquals("ACCEPTED", reservation.getStatusLabel());
    }

    @Test
    void testReservationConstructor() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Reservation reservation = new Reservation(5, 2, 3, ReservationStatus.DENIED, now, false);

        assertEquals(5, reservation.getId());
        assertEquals(2, reservation.getUserId());
        assertEquals(3, reservation.getEventId());
        assertEquals(ReservationStatus.DENIED, reservation.getStatus());
        assertEquals(now, reservation.getReservedAt());
        assertFalse(reservation.isQrCodeChecked());
    }
}
