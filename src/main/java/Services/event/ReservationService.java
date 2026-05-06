package Services.event;

import Entities.User.User;
import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Repositories.event.JdbcReservationRepository;
import Repositories.event.ReservationRepository;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();
            Event event = eventService.getById(reservation.getEventId());

            PdfPTable wrapper = new PdfPTable(1);
            wrapper.setWidthPercentage(80);
            wrapper.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell card = new PdfPCell();
            card.setBorder(Rectangle.NO_BORDER);
            card.setPadding(18f);
            card.setCellEvent(new DashedBorder());

            Image logo = loadLogo();
            if (logo != null) {
                logo.scaleToFit(90f, 90f);
                logo.setAlignment(Element.ALIGN_LEFT);
                card.addElement(logo);
            }

            Paragraph title = new Paragraph("Event Ticket", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setSpacingBefore(4f);
            title.setSpacingAfter(6f);
            card.addElement(title);

            PdfPTable accent = new PdfPTable(1);
            accent.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setFixedHeight(3f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineCell.setBackgroundColor(new java.awt.Color(126, 84, 255));
            accent.addCell(lineCell);
            card.addElement(accent);

            PdfPTable details = buildTicketDetailsTable(reservation, event);
            details.setSpacingBefore(10f);
            card.addElement(details);

            Image qr = loadQrImage(event);
            if (qr != null) {
                qr.scaleToFit(110f, 110f);
                qr.setAlignment(Element.ALIGN_RIGHT);
                card.addElement(qr);
            }

            wrapper.addCell(card);
            document.add(wrapper);
        } catch (DocumentException | IOException | SQLException e) {
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

    private PdfPTable buildTicketDetailsTable(Reservation reservation, Event event) {
        PdfPTable details = new PdfPTable(new float[]{1.2f, 2.8f});
        details.setWidthPercentage(100);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new java.awt.Color(70, 70, 70));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, java.awt.Color.BLACK);

        addDetailRow(details, "Reservation ID", String.valueOf(reservation.getId()), labelFont, valueFont);
        addDetailRow(details, "Event", safe(reservation.getEventTitle()), labelFont, valueFont);
        addDetailRow(details, "Attendee", safe(reservation.getUsername()), labelFont, valueFont);
        addDetailRow(details, "Email", safe(reservation.getUserEmail()), labelFont, valueFont);
        addDetailRow(details, "Phone", safe(reservation.getUserPhone()), labelFont, valueFont);
        addDetailRow(details, "Reserved At", String.valueOf(reservation.getReservedAt()), labelFont, valueFont);

        if (event != null) {
            addDetailRow(details, "Location", safe(event.getLocation()), labelFont, valueFont);
            addDetailRow(details, "Schedule", formatEventWindow(event.getStartDate(), event.getEndDate()), labelFont, valueFont);
        }

        addDetailRow(details, "Status", safe(reservation.getStatusLabel()), labelFont, valueFont);
        return details;
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label + ":", labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6f);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatEventWindow(Timestamp start, Timestamp end) {
        if (start == null && end == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
        String startText = start == null ? "N/A" : formatter.format(start.toInstant().atZone(ZoneId.systemDefault()));
        String endText = end == null ? "N/A" : formatter.format(end.toInstant().atZone(ZoneId.systemDefault()));
        return startText + " -> " + endText;
    }

    private Image loadLogo() {
        try {
            Path logoPath = Paths.get(System.getProperty("user.dir"), "assets", "images", "logo", "shadow-logo.png");
            if (!Files.exists(logoPath)) {
                return null;
            }
            return Image.getInstance(logoPath.toAbsolutePath().toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Image loadQrImage(Event event) {
        if (event == null || event.getQrCodePath() == null || event.getQrCodePath().isBlank()) {
            return null;
        }

        String rawPath = event.getQrCodePath().trim();
        try {
            Path resolved;
            if (rawPath.startsWith("/uploads/")) {
                resolved = Paths.get(System.getProperty("user.dir") + rawPath);
            } else {
                resolved = Paths.get(rawPath);
            }
            if (!Files.exists(resolved)) {
                return null;
            }
            return Image.getInstance(resolved.toAbsolutePath().toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static final class DashedBorder implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvas) {
            PdfContentByte cb = canvas[PdfPTable.LINECANVAS];
            cb.saveState();
            cb.setLineWidth(1f);
            cb.setLineDash(4f, 3f);
            cb.setColorStroke(new java.awt.Color(126, 84, 255));
            cb.rectangle(rect.getLeft() + 1.5f, rect.getBottom() + 1.5f, rect.getWidth() - 3f, rect.getHeight() - 3f);
            cb.stroke();
            cb.restoreState();
        }
    }
}
