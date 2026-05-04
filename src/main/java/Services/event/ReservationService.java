package Services.event;

import Entities.User.User;
import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.JdbcReservationRepository;
import Repositories.event.ReservationRepository;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventService eventService;
    private final ReservationNotificationGateway notificationGateway;

    public ReservationService() {
        this(new JdbcReservationRepository(), new EventService(), new ReservationNotifierService());
    }

    public ReservationService(ReservationRepository reservationRepository,
                              EventService eventService,
                              ReservationNotificationGateway notificationGateway) {
        this.reservationRepository = reservationRepository;
        this.eventService = eventService;
        this.notificationGateway = notificationGateway;
    }

    public Reservation reserve(int userId, int eventId) {
        if (userId <= 0 || eventId <= 0) {
            throw new EventModuleException("Invalid reservation payload.");
        }

        try {
            Event event = eventService.getById(eventId);
            if (event == null) {
                throw new EventModuleException("Event not found.");
            }
            if (event.getStatus() == null || !"ACTIVE".equalsIgnoreCase(event.getStatus())) {
                throw new EventModuleException("Only ACTIVE events can be reserved.");
            }

            Optional<Reservation> existing = reservationRepository.findByUserAndEvent(userId, eventId);
            if (existing.isPresent()) {
                throw new EventModuleException("User already has a reservation for this event.");
            }

            return reservationRepository.createPending(userId, eventId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to create reservation: " + e.getMessage(), e);
        }
    }

    public Optional<Reservation> getUserReservation(int userId, int eventId) {
        try {
            return reservationRepository.findByUserAndEvent(userId, eventId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to read reservation: " + e.getMessage(), e);
        }
    }

    public List<Reservation> getAcceptedReservationsForUser(int userId) {
        try {
            if (userId <= 0) {
                return Collections.emptyList();
            }
            return reservationRepository.findAcceptedByUser(userId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to load accepted reservations: " + e.getMessage(), e);
        }
    }

    public int notifyEventReschedule(Event event, Timestamp oldStart, Timestamp oldEnd) {
        if (event == null || event.getId() <= 0) {
            return 0;
        }

        try {
            List<Reservation> reservations = reservationRepository.findByEvent(event.getId());
            int notified = 0;
            for (Reservation reservation : reservations) {
                if (reservation.getStatus() == ReservationStatus.DENIED) {
                    continue;
                }
                notificationGateway.notifyEventReschedule(reservation, event, oldStart, oldEnd);
                notified++;
            }
            return notified;
        } catch (SQLException e) {
            throw new EventModuleException("Unable to notify reservations: " + e.getMessage(), e);
        }
    }

    public List<Reservation> findBackOfficeReservations(String search, String sortBy, boolean ascending, int page, int pageSize) {
        try {
            int safePage = Math.max(1, page);
            int safePageSize = Math.max(1, Math.min(100, pageSize));
            int offset = (safePage - 1) * safePageSize;
            return reservationRepository.findForBackOffice(search, sortBy, ascending, offset, safePageSize);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to load reservation list: " + e.getMessage(), e);
        }
    }

    public int countBackOfficeReservations(String search) {
        try {
            return reservationRepository.countForBackOffice(search);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to count reservations: " + e.getMessage(), e);
        }
    }

    public Reservation approve(int reservationId, User actor) {
        return moderateReservation(reservationId, actor, ReservationStatus.ACCEPTED);
    }

    public Reservation reject(int reservationId, User actor) {
        return moderateReservation(reservationId, actor, ReservationStatus.DENIED);
    }

    public void delete(int reservationId, User actor) {
        enforceAdmin(actor);
        try {
            reservationRepository.delete(reservationId);
        } catch (SQLException e) {
            throw new EventModuleException("Unable to delete reservation: " + e.getMessage(), e);
        }
    }

    public void generateTicketPdf(int reservationId, User actor, Path outputPath) {
        Reservation reservation = requireOwnerAcceptedReservation(reservationId, actor);

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();
            document.add(new Paragraph("Shadow Dimensions - Event Ticket"));
            document.add(new Paragraph("Reservation ID: " + reservation.getId()));
            document.add(new Paragraph("Event: " + reservation.getEventTitle()));
            document.add(new Paragraph("User: " + safe(reservation.getUsername())));
            document.add(new Paragraph("Reserved At: " + reservation.getReservedAt()));
            document.add(new Paragraph("Status: " + reservation.getStatusLabel()));
        } catch (DocumentException | IOException e) {
            throw new EventModuleException("Unable to generate ticket PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    public void generateIcs(int reservationId, User actor, Path outputPath) {
        Reservation reservation = requireOwnerAcceptedReservation(reservationId, actor);

        DateTimeFormatter formatUtc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String startUtc = formatUtc.format(reservation.getEventStartDate().toInstant().atZone(ZoneId.of("UTC")));
        String endUtc = formatUtc.format(reservation.getEventEndDate().toInstant().atZone(ZoneId.of("UTC")));

        String ics = "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//ShadowDimensions//Events//EN\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:reservation-" + reservation.getId() + "@shadowdimensions\r\n"
                + "DTSTAMP:" + startUtc + "\r\n"
                + "DTSTART:" + startUtc + "\r\n"
                + "DTEND:" + endUtc + "\r\n"
                + "SUMMARY:" + escapeIcs(reservation.getEventTitle()) + "\r\n"
                + "DESCRIPTION:Shadow Dimensions event ticket\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";

        try {
            Files.writeString(outputPath, ics, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EventModuleException("Unable to generate ICS file: " + e.getMessage(), e);
        }
    }

    private Reservation moderateReservation(int reservationId, User actor, ReservationStatus status) {
        enforceAdmin(actor);
        try {
            Reservation existing = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new EventModuleException("Reservation not found."));

            reservationRepository.updateStatus(reservationId, status);
            Reservation updated = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new EventModuleException("Reservation not found after moderation."));
            notificationGateway.notifyReservationDecision(updated, status);
            return updated;
        } catch (SQLException e) {
            throw new EventModuleException("Unable to moderate reservation: " + e.getMessage(), e);
        }
    }

    private Reservation requireOwnerAcceptedReservation(int reservationId, User actor) {
        if (actor == null) {
            throw new EventModuleException("Authentication required.");
        }

        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new EventModuleException("Reservation not found."));

            if (reservation.getUserId() != actor.getId()) {
                throw new EventModuleException("Only reservation owner can access ticket and ICS.");
            }
            if (reservation.getStatus() != ReservationStatus.ACCEPTED) {
                throw new EventModuleException("Ticket and ICS are available for ACCEPTED reservations only.");
            }
            return reservation;
        } catch (SQLException e) {
            throw new EventModuleException("Unable to validate reservation ownership: " + e.getMessage(), e);
        }
    }

    private void enforceAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new EventModuleException("Admin privileges required.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String escapeIcs(String value) {
        if (value == null) {
            return "Event";
        }
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}
