package Services.event;

import Entities.event.Category;
import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.ReservationRepository;
import Repositories.event.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Test
    void reviewEligibilityRuleBlockedWhenEventNotPast() throws Exception {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventService eventService = mock(EventService.class);

        Reservation reservation = new Reservation();
        reservation.setId(5);
        reservation.setUserId(1);
        reservation.setEventId(2);
        reservation.setStatus(ReservationStatus.ACCEPTED);

        when(reservationRepository.findByUserAndEvent(1, 2)).thenReturn(Optional.of(reservation));

        Event event = new Event();
        event.setId(2);
        event.setStatus("ACTIVE");
        event.setCategory(new Category());
        event.setEndDate(Timestamp.from(Instant.now().plusSeconds(3600)));
        when(eventService.getById(2)).thenReturn(event);

        ReviewService service = new ReviewService(reviewRepository, reservationRepository, eventService);

        assertThrows(EventModuleException.class, () -> service.createOrUpdate(1, 2, 5, "Great"));
        verify(reviewRepository, never()).create(any());
        verify(reviewRepository, never()).update(any());
    }
}
