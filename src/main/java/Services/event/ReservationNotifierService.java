package Services.event;

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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

public class ReservationNotifierService implements ReservationNotificationGateway {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public void notifyReservationDecision(Reservation reservation, ReservationStatus newStatus) {
        sendEmail(reservation, newStatus);
        sendSms(reservation, newStatus);
    }

    private void sendEmail(Reservation reservation, ReservationStatus status) {
        if (reservation.getUserEmail() == null || reservation.getUserEmail().isBlank()) {
            return;
        }

        String mailDsn = AppConfig.get("MAILER_DSN");
        String smtpHost = "smtp.gmail.com";
        String smtpPort = "587";
        String username = AppConfig.get("MAIL_USERNAME");
        String password = AppConfig.get("MAIL_PASSWORD");

        if ((username == null || password == null) && mailDsn != null && mailDsn.contains("://")) {
            ParsedMailer parsed = parseMailerDsn(mailDsn);
            username = parsed.username();
            password = parsed.password();
        }

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        String finalUsername = username;
        String finalPassword = password.replaceAll("\\s+", "");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalUsername, finalPassword);
            }
        });

        String safeName = reservation.getUsername() == null || reservation.getUsername().isBlank()
                ? "Shadow Dweller"
                : reservation.getUsername();
        String body = "Hello " + safeName + ",\n\n"
                + "Your reservation status has been updated for event: " + reservation.getEventTitle() + "\n"
                + "New status: " + status.name() + "\n\n"
                + "Thank you for using Shadow Dimensions.";

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(finalUsername));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getUserEmail()));
            message.setSubject("Shadow Dimensions - Reservation " + status.name());
            message.setText(body);
            Transport.send(message);
        } catch (Exception ignored) {
            // Notifications are best-effort and should not block moderation actions.
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

        String body = "Shadow Dimensions: your reservation for '" + reservation.getEventTitle() + "' is now " + status.name() + ".";

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

    private ParsedMailer parseMailerDsn(String dsn) {
        String normalized = dsn;
        int protocolIndex = normalized.indexOf("://");
        if (protocolIndex >= 0) {
            normalized = normalized.substring(protocolIndex + 3);
        }

        int atIndex = normalized.indexOf('@');
        String credentials = atIndex > 0 ? normalized.substring(0, atIndex) : normalized;
        int sep = credentials.indexOf(':');
        if (sep <= 0) {
            return new ParsedMailer("", "");
        }

        String user = credentials.substring(0, sep);
        String pass = credentials.substring(sep + 1);
        return new ParsedMailer(user, pass);
    }

    private record ParsedMailer(String username, String password) {
    }
}
