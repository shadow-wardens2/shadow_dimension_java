package Services.event;

import Entities.User.User;
import Entities.event.EventReclamation;
import Entities.event.EventReclamationStatus;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.EventReclamationRepository;
import Repositories.event.JdbcEventReclamationRepository;
import Repositories.event.JdbcReservationRepository;
import Repositories.event.ReservationRepository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EventReclamationService {

    private final EventReclamationRepository reclamationRepository;
    private final ReservationRepository reservationRepository;
    private final EventAiAssistantService aiAssistantService;

    public EventReclamationService() {
        this(new JdbcEventReclamationRepository(), new JdbcReservationRepository(), new EventAiAssistantService());
    }

    public EventReclamationService(EventReclamationRepository reclamationRepository,
                                   ReservationRepository reservationRepository,
                                   EventAiAssistantService aiAssistantService) {
        this.reclamationRepository = reclamationRepository;
        this.reservationRepository = reservationRepository;
        this.aiAssistantService = aiAssistantService;
    }

    public EventReclamation create(int userId, int eventId, String subject, String message) {
        String cleanSubject = subject == null ? "" : subject.trim();
        String cleanMessage = message == null ? "" : message.trim();
        if (userId <= 0 || eventId <= 0 || cleanSubject.isBlank() || cleanMessage.isBlank()) {
            throw new EventModuleException("Invalid complaint payload.");
        }
        if (cleanSubject.length() < 3) {
            throw new EventModuleException("Complaint subject must be at least 3 characters.");
        }
        if (cleanMessage.length() < 10) {
            throw new EventModuleException("Complaint message must be at least 10 characters.");
        }

        try {
            Reservation reservation = reservationRepository.findByUserAndEvent(userId, eventId)
                    .orElseThrow(() -> new EventModuleException("Only users with reservations can submit a complaint for this event."));
            if (reservation.getStatus() != ReservationStatus.ACCEPTED) {
                throw new EventModuleException("Only ACCEPTED reservations can open complaints.");
            }

            Optional<EventReclamation> existing = reclamationRepository.findOpenByUserAndEvent(userId, eventId);
            if (existing.isPresent()) {
                throw new EventModuleException("You already have an active complaint for this event.");
            }

            EventReclamation reclamation = new EventReclamation();
            reclamation.setUserId(userId);
            reclamation.setEventId(eventId);
            reclamation.setStatus(EventReclamationStatus.OPEN);
            reclamation.setSubject(cleanSubject);
            reclamation.setMessage(cleanMessage);
            reclamation.setAiResponse(buildAiSummaryFallback(cleanSubject, cleanMessage, reservation.getEventTitle()));
            Timestamp now = new Timestamp(System.currentTimeMillis());
            reclamation.setCreatedAt(now);
            reclamation.setUpdatedAt(now);

            return reclamationRepository.create(reclamation);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to create complaint: " + e.getMessage(), e);
        }
    }

    public List<EventReclamation> findMyReclamations(int userId) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return reclamationRepository.findByUser(userId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to load your complaints: " + e.getMessage(), e);
        }
    }

    public EventReclamation escalate(int reclamationId, User actor) {
        if (actor == null) {
            throw new EventModuleException("Authentication required.");
        }
        try {
            EventReclamation row = reclamationRepository.findById(reclamationId)
                    .orElseThrow(() -> new EventModuleException("Complaint not found."));
            if (row.getUserId() != actor.getId()) {
                throw new EventModuleException("Only the complaint owner can escalate it.");
            }
            if (!row.canEscalate()) {
                throw new EventModuleException("This complaint cannot be escalated from the current state.");
            }

            reclamationRepository.escalate(reclamationId);
            return reclamationRepository.findById(reclamationId)
                    .orElseThrow(() -> new EventModuleException("Complaint not found after escalation."));
        } catch (SQLException e) {
            throw new EventModuleException("Unable to escalate complaint: " + e.getMessage(), e);
        }
    }

    public EventReclamation adminRespond(int reclamationId, EventReclamationStatus targetStatus, String response, User actor) {
        enforceAdmin(actor);
        if (targetStatus == null) {
            throw new EventModuleException("Status is required.");
        }
        if (targetStatus == EventReclamationStatus.OPEN || targetStatus == EventReclamationStatus.ESCALATED) {
            throw new EventModuleException("Admin response must move the complaint to IN_PROGRESS, RESOLVED, or REJECTED.");
        }

        String cleanResponse = response == null ? "" : response.trim();
        if (cleanResponse.isBlank()) {
            throw new EventModuleException("Admin response is required.");
        }

        try {
            EventReclamation existing = reclamationRepository.findById(reclamationId)
                    .orElseThrow(() -> new EventModuleException("Complaint not found."));

            if (existing.getStatus() == EventReclamationStatus.RESOLVED || existing.getStatus() == EventReclamationStatus.REJECTED) {
                throw new EventModuleException("Resolved or rejected complaints cannot be changed.");
            }

            reclamationRepository.adminRespond(reclamationId, targetStatus, cleanResponse);
            return reclamationRepository.findById(reclamationId)
                    .orElseThrow(() -> new EventModuleException("Complaint not found after update."));
        } catch (SQLException e) {
            throw new EventModuleException("Unable to update complaint: " + e.getMessage(), e);
        }
    }

    public List<EventReclamation> findBackOfficeRows(String search, String status, String sortBy, boolean ascending, int page, int pageSize, User actor) {
        enforceAdmin(actor);
        try {
            int safePage = Math.max(1, page);
            int safePageSize = Math.max(1, Math.min(100, pageSize));
            int offset = (safePage - 1) * safePageSize;
            return reclamationRepository.findForBackOffice(search, status, sortBy, ascending, offset, safePageSize);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to load complaints: " + e.getMessage(), e);
        }
    }

    public int countBackOfficeRows(String search, String status, User actor) {
        enforceAdmin(actor);
        try {
            return reclamationRepository.countForBackOffice(search, status);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to count complaints: " + e.getMessage(), e);
        }
    }

    public String buildAiAdminReplySuggestion(EventReclamation reclamation) {
        if (reclamation == null) {
            return "Please provide a respectful resolution response and explain the next step clearly.";
        }

        String prompt = "Suggest a short admin response for this event complaint. "
                + "Status=" + reclamation.getStatusLabel() + ", Event='" + safe(reclamation.getEventTitle()) + "', "
            + "Subject='" + safe(reclamation.getSubject()) + "', Message='" + safe(reclamation.getMessage()) + "'. "
                + "Keep it under 70 words and include one concrete next action.";

        String raw = aiAssistantService.askQuestion(prompt);
        if (raw == null || raw.isBlank() || raw.startsWith("AI key missing") || raw.startsWith("AI service error") || raw.startsWith("Failed to call AI service")) {
            return "Thank you for your report. We started handling this complaint and will update you with a concrete resolution step shortly.";
        }
        return raw.trim();
    }

    public void deleteReclamation(int reclamationId, User actor) {
        enforceAdmin(actor);
        if (reclamationId <= 0) {
            throw new EventModuleException("Invalid complaint ID.");
        }
        try {
            reclamationRepository.deleteById(reclamationId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to delete complaint: " + e.getMessage(), e);
        }
    }

    private String buildAiSummaryFallback(String subject, String message, String eventTitle) {
        String prompt = "Summarize this event complaint in one sentence for admins. Event='" + safe(eventTitle)
                + "', Subject='" + safe(subject) + "', Message='" + safe(message) + "'.";
        String raw = aiAssistantService.askQuestion(prompt);
        if (raw == null || raw.isBlank() || raw.startsWith("AI key missing") || raw.startsWith("AI service error") || raw.startsWith("Failed to call AI service")) {
            String cut = message.length() > 120 ? message.substring(0, 120) + "..." : message;
            return "Complaint summary: " + cut;
        }
        return raw.trim();
    }

    private void enforceAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new EventModuleException("Admin privileges required.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value.replaceAll("\\s+", " ").trim();
    }
}
