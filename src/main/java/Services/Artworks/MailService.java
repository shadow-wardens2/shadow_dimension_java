package Services.Artworks;

import Utils.EnvConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class MailService {

    private String username;
    private String password;
    private String from;
    private String host;
    private String port;

    public MailService() {
        this.username = EnvConfig.get("MAIL_USERNAME", "");
        this.password = EnvConfig.get("MAIL_PASSWORD", "");
        this.from = EnvConfig.get("MAIL_FROM", "no-reply@shadowdimension.tn");
        this.host = EnvConfig.get("MAIL_SMTP_HOST", "smtp.gmail.com");
        this.port = EnvConfig.get("MAIL_SMTP_PORT", "587");
    }

    public void sendReservationEmail(String recipientEmail, String artworkTitle, int price) throws MessagingException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("🔮 SIMULATION MODE: No SMTP config found. Manifesting fake scroll to: " + recipientEmail);
            System.out.println("Relic: " + artworkTitle + " | Value: " + price + " DT");
            try { Thread.sleep(1500); } catch (InterruptedException e) {} // Simulate delay
            return;
        }

        System.out.println("📬 Attempting to manifest reservation email for " + recipientEmail + " via " + host + ":" + port);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", "true"); // Enable this to see detailed logs in console

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("🌌 Shadow Dimension - Reservation Manifested");

            String content = "<div style='font-family: Arial, sans-serif; background-color: #050507; color: #e8e0eb; padding: 40px; border-radius: 20px;'>"
                    + "<h2 style='color: #8b5cf6;'>Greetings from the Shadow Dimension</h2>"
                    + "<p>Your reservation for the relic <b>" + artworkTitle + "</b> has been successfully recorded.</p>"
                    + "<p style='font-size: 18px;'><b>Market Value:</b> <span style='color: #10b981;'>" + price + " DT</span></p>"
                    + "<p>The dimension awaits your final ritual for acquisition.</p>"
                    + "<hr style='border: 0.5px solid #1a1a24;'>"
                    + "<p style='font-size: 12px; color: #adaaae;'><i>Manifested by the Shadow Curator</i></p>"
                    + "</div>";

            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ SUCCESS: Reservation email sent to " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("❌ FAILED: Could not manifest email. Error: " + e.getMessage());
            throw e;
        }
    }
}
