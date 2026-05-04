package Services.event;

import Entities.event.Event;
import Entities.event.Reservation;
import Entities.event.ReservationStatus;
import Utils.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

public class ReservationNotifierService implements ReservationNotificationGateway {

    private final HttpClient client = HttpClient.newHttpClient();

    public ReservationNotifierService() {
        AppConfig.loadDotEnv();
    }

    @Override
    public void notifyReservationDecision(Reservation reservation, ReservationStatus newStatus) {
        sendEmail(reservation, newStatus);
        sendSms(reservation, newStatus);
    }

    @Override
    public void notifyEventReschedule(Reservation reservation, Event event, Timestamp oldStart, Timestamp oldEnd) {
        sendRescheduleEmail(reservation, event, oldStart, oldEnd);
        sendRescheduleSms(reservation, event, oldStart, oldEnd);
    }

    private void sendEmail(Reservation reservation, ReservationStatus status) {
        if (reservation.getUserEmail() == null || reservation.getUserEmail().isBlank()) {
            return;
        }

        MailSettings settings = resolveMailSettings();
        if (settings == null) {
            return;
        }

        Session session = createMailSession(settings);

        String safeName = reservation.getUsername() == null || reservation.getUsername().isBlank()
            ? "Shadow Dweller"
            : reservation.getUsername();

        String subject;
        String body;
        if (status == ReservationStatus.ACCEPTED) {
            subject = "Reservation Approved";
            body = "Hello " + safeName + ",\n\n"
                + "Your reservation for the event '" + reservation.getEventTitle() + "' has been approved successfully. "
                + "We look forward to seeing you.\n\n"
                + "Thank you for using Shadow Dimensions.";
        } else if (status == ReservationStatus.DENIED) {
            subject = "Reservation Update";
            body = "Hello " + safeName + ",\n\n"
                + "Your reservation for the event '" + reservation.getEventTitle() + "' could not be approved at this time. "
                + "Please contact our team for more information.\n\n"
                + "Thank you for using Shadow Dimensions.";
        } else {
            subject = "Shadow Dimensions - Reservation " + status.name();
            body = "Hello " + safeName + ",\n\n"
                + "Your reservation status has been updated for event: " + reservation.getEventTitle() + "\n"
                + "New status: " + status.name() + "\n\n"
                + "Thank you for using Shadow Dimensions.";
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.from()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getUserEmail()));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            System.err.println("Reservation email failed: " + e.getMessage());
        }
    }

    private void sendRescheduleEmail(Reservation reservation, Event event, Timestamp oldStart, Timestamp oldEnd) {
        if (reservation == null || event == null) {
            return;
        }
        if (reservation.getUserEmail() == null || reservation.getUserEmail().isBlank()) {
            return;
        }

        MailSettings settings = resolveMailSettings();
        if (settings == null) {
            return;
        }

        Session session = createMailSession(settings);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String zone = ZoneId.systemDefault().getId();

        String safeName = reservation.getUsername() == null || reservation.getUsername().isBlank()
                ? "Shadow Dweller"
                : reservation.getUsername();
        String safeTitle = event.getTitle() == null || event.getTitle().isBlank()
                ? reservation.getEventTitle()
                : event.getTitle();
        String safeLocation = event.getLocation() == null || event.getLocation().isBlank()
                ? "TBA"
                : event.getLocation();

        String oldStartText = oldStart == null ? "TBA" : formatter.format(oldStart.toInstant().atZone(ZoneId.systemDefault()));
        String oldEndText = oldEnd == null ? "TBA" : formatter.format(oldEnd.toInstant().atZone(ZoneId.systemDefault()));
        String newStartText = event.getStartDate() == null ? "TBA" : formatter.format(event.getStartDate().toInstant().atZone(ZoneId.systemDefault()));
        String newEndText = event.getEndDate() == null ? "TBA" : formatter.format(event.getEndDate().toInstant().atZone(ZoneId.systemDefault()));

        String subject = "Event Rescheduled: " + safeTitle;
        String body = "Hello " + safeName + ",\n\n"
                + "The event '" + safeTitle + "' has a new date.\n"
                + "Old schedule: " + oldStartText + " to " + oldEndText + " (" + zone + ")\n"
                + "New schedule: " + newStartText + " to " + newEndText + " (" + zone + ")\n"
                + "Location: " + safeLocation + "\n\n"
                + "If you can no longer attend, please contact the organizers.\n\n"
                + "Thank you for using Shadow Dimensions.";

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.from()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getUserEmail()));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            System.err.println("Reschedule email failed: " + e.getMessage());
        }
    }

    private void sendRescheduleSms(Reservation reservation, Event event, Timestamp oldStart, Timestamp oldEnd) {
        String to = reservation.getUserPhone();
        if (to == null || to.isBlank()) {
            return;
        }

        String sid = AppConfig.get("TWILIO_ACCOUNT_SID");
        String auth = AppConfig.get("TWILIO_AUTH_TOKEN");
        String from = AppConfig.get("TWILIO_FROM_NUMBER");

        if (sid == null || auth == null || from == null || sid.isBlank() || auth.isBlank() || from.isBlank()) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String newStartText = event.getStartDate() == null ? "TBA" : formatter.format(event.getStartDate().toInstant().atZone(ZoneId.systemDefault()));
        
        String body = "Shadow Dimensions: Event '" + event.getTitle() + "' has been rescheduled to " + newStartText + ". Check your email for details.";

        String payload = "To=" + urlEncode(to)
                + "&From=" + urlEncode(from)
                + "&Body=" + urlEncode(body);

        String authHeader = Base64.getEncoder()
                .encodeToString((sid + ":" + auth).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Basic " + authHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendSms(Reservation reservation, ReservationStatus status) {
        String to = reservation.getUserPhone();
        if (to == null || to.isBlank()) {
            return;
        }

        String sid = AppConfig.get("TWILIO_ACCOUNT_SID");
        String auth = AppConfig.get("TWILIO_AUTH_TOKEN");
        String from = AppConfig.get("TWILIO_FROM_NUMBER");

        if (sid == null || auth == null || from == null || sid.isBlank() || auth.isBlank() || from.isBlank()) {
            return;
        }

        String safeName = reservation.getUsername() == null || reservation.getUsername().isBlank()
                ? "there"
                : reservation.getUsername();

        String body;
        if (status == ReservationStatus.ACCEPTED) {
            body = "Hello " + safeName + "! Your reservation for '" + reservation.getEventTitle() + "' has been approved successfully! We look forward to seeing you.";
        } else if (status == ReservationStatus.DENIED) {
            body = "Hello " + safeName + ". Unfortunately, your reservation for '" + reservation.getEventTitle() + "' could not be approved at this time. Please contact us for more information.";
        } else {
            body = "Shadow Dimensions: your reservation for '" + reservation.getEventTitle() + "' is now " + status.name() + ".";
        }

        String payload = "To=" + urlEncode(to)
                + "&From=" + urlEncode(from)
                + "&Body=" + urlEncode(body);

        String authHeader = Base64.getEncoder()
                .encodeToString((sid + ":" + auth).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Basic " + authHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record MailSettings(String username, String password, String from, String smtpHost, String smtpPort) {
    }

    private MailSettings resolveMailSettings() {
        String smtpHost = AppConfig.getOrDefault("MAIL_SMTP_HOST", "smtp.gmail.com");
        String smtpPort = AppConfig.getOrDefault("MAIL_SMTP_PORT", "587");
        String username = AppConfig.get("MAIL_USERNAME");
        if (username == null || username.isBlank()) {
            username = AppConfig.get("MAIL_FROM");
        }
        String password = AppConfig.get("MAIL_PASSWORD");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        String from = AppConfig.getOrDefault("MAIL_FROM", username);
        return new MailSettings(username, password.replaceAll("\\s+", ""), from, smtpHost, smtpPort);
    }

    private Session createMailSession(MailSettings settings) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", settings.smtpHost());
        props.put("mail.smtp.port", settings.smtpPort());
        props.put("mail.smtp.ssl.trust", settings.smtpHost());
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(settings.username(), settings.password());
            }
        });
    }
}
