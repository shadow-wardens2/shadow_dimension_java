package Services.event;

import Entities.User.User;
import Entities.event.EventReclamation;
import Entities.event.EventReclamationStatus;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.EventReclamationRepository;
import Repositories.event.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class EventReclamationServiceTest {

    @Test
    void duplicateActiveReclamationBlocked() throws Exception {
        EventReclamationRepository reclamationRepository = mock(EventReclamationRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventAiAssistantService aiAssistantService = mock(EventAiAssistantService.class);

        Reservation accepted = new Reservation();
        accepted.setId(5);
        accepted.setUserId(2);
        accepted.setEventId(11);
        accepted.setStatus(ReservationStatus.ACCEPTED);
        accepted.setEventTitle("Night Arena");

        when(reservationRepository.findByUserAndEvent(2, 11)).thenReturn(Optional.of(accepted));

        EventReclamation existing = new EventReclamation();
        existing.setId(30);
        existing.setUserId(2);
        existing.setEventId(11);
        existing.setStatus(EventReclamationStatus.OPEN);
        when(reclamationRepository.findOpenByUserAndEvent(2, 11)).thenReturn(Optional.of(existing));

        EventReclamationService service = new EventReclamationService(reclamationRepository, reservationRepository, aiAssistantService);

        assertThrows(EventModuleException.class, () -> service.create(2, 11, "Check-in issue", "Issue with event handling and check-in process"));
        verify(reclamationRepository, never()).create(any());
    }

    @Test
    void nonOwnerCannotEscalate() throws Exception {
        EventReclamationRepository reclamationRepository = mock(EventReclamationRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventAiAssistantService aiAssistantService = mock(EventAiAssistantService.class);

        EventReclamation row = new EventReclamation();
        row.setId(9);
        row.setUserId(2);
        row.setEventId(11);
        row.setStatus(EventReclamationStatus.OPEN);

        when(reclamationRepository.findById(9)).thenReturn(Optional.of(row));

        EventReclamationService service = new EventReclamationService(reclamationRepository, reservationRepository, aiAssistantService);

        User actor = new User();
        actor.setId(88);
        actor.setRoles("[\"ROLE_USER\"]");

        assertThrows(EventModuleException.class, () -> service.escalate(9, actor));
        verify(reclamationRepository, never()).escalate(anyInt());
    }

    @Test
    void adminCannotUpdateTerminalReclamation() throws Exception {
        EventReclamationRepository reclamationRepository = mock(EventReclamationRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventAiAssistantService aiAssistantService = mock(EventAiAssistantService.class);

        EventReclamation row = new EventReclamation();
        row.setId(17);
        row.setUserId(2);
        row.setEventId(11);
        row.setStatus(EventReclamationStatus.RESOLVED);

        when(reclamationRepository.findById(17)).thenReturn(Optional.of(row));

        EventReclamationService service = new EventReclamationService(reclamationRepository, reservationRepository, aiAssistantService);

        User admin = new User();
        admin.setId(1);
        admin.setRoles("[\"ROLE_ADMIN\"]");

        assertThrows(EventModuleException.class,
                () -> service.adminRespond(17, EventReclamationStatus.REJECTED, "Closing", admin));

        verify(reclamationRepository, never()).adminRespond(anyInt(), any(), any());
    }
}
