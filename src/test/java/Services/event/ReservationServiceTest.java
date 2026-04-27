package Services.event;

import Entities.User.User;
import Entities.event.Category;
import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    @Test
    void duplicateReservationBlocked() throws Exception {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventService eventService = mock(EventService.class);
        ReservationNotificationGateway notifier = mock(ReservationNotificationGateway.class);

        Event event = buildEvent("ACTIVE", Timestamp.from(Instant.now().minusSeconds(3600)), Timestamp.from(Instant.now().plusSeconds(3600)));
        when(eventService.getById(7)).thenReturn(event);

        Reservation existing = new Reservation();
        existing.setId(99);
        existing.setUserId(1);
        existing.setEventId(7);
        existing.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.findByUserAndEvent(1, 7)).thenReturn(Optional.of(existing));

        ReservationService service = new ReservationService(reservationRepository, eventService, notifier);

        assertThrows(EventModuleException.class, () -> service.reserve(1, 7));
        verify(reservationRepository, never()).createPending(anyInt(), anyInt());
    }

    @Test
    void ownerOnlyTicketAccessEnforced() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventService eventService = mock(EventService.class);
        ReservationNotificationGateway notifier = mock(ReservationNotificationGateway.class);

        Reservation reservation = new Reservation();
        reservation.setId(30);
        reservation.setUserId(2);
        reservation.setEventId(7);
        reservation.setStatus(ReservationStatus.ACCEPTED);
        reservation.setEventTitle("Moon Ritual");
        reservation.setEventStartDate(Timestamp.from(Instant.now().plusSeconds(3600)));
        reservation.setEventEndDate(Timestamp.from(Instant.now().plusSeconds(7200)));

        try {
            when(reservationRepository.findById(30)).thenReturn(Optional.of(reservation));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ReservationService service = new ReservationService(reservationRepository, eventService, notifier);

        User actor = new User();
        actor.setId(9);
        actor.setRoles("[\"ROLE_USER\"]");

        assertThrows(EventModuleException.class,
                () -> service.generateIcs(30, actor, java.nio.file.Path.of("ticket.ics")));
    }

    @Test
    void adminApproveRejectFlow() throws Exception {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventService eventService = mock(EventService.class);
        ReservationNotificationGateway notifier = mock(ReservationNotificationGateway.class);

        Reservation row = new Reservation();
        row.setId(10);
        row.setUserId(3);
        row.setEventId(7);
        row.setStatus(ReservationStatus.PENDING);
        row.setEventTitle("Night Arena");
        row.setReservedAt(Timestamp.from(Instant.now()));

        Reservation accepted = new Reservation();
        accepted.setId(10);
        accepted.setUserId(3);
        accepted.setEventId(7);
        accepted.setStatus(ReservationStatus.ACCEPTED);
        accepted.setEventTitle("Night Arena");

        when(reservationRepository.findById(10)).thenReturn(Optional.of(row), Optional.of(accepted));

        ReservationService service = new ReservationService(reservationRepository, eventService, notifier);

        User admin = new User();
        admin.setId(1);
        admin.setRoles("[\"ROLE_ADMIN\"]");

        service.approve(10, admin);

        verify(reservationRepository).updateStatus(10, ReservationStatus.ACCEPTED);
        verify(notifier).notifyReservationDecision(any(Reservation.class), eq(ReservationStatus.ACCEPTED));
    }

    private Event buildEvent(String status, Timestamp start, Timestamp end) {
        Event event = new Event();
        event.setId(7);
        event.setTitle("Event");
        event.setStatus(status);
        event.setStartDate(start);
        event.setEndDate(end);
        event.setCategory(new Category());
        return event;
    }
}
