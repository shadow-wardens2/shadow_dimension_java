package Services.User;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Sends one-time verification code using SMTP settings from environment variables.
    public void sendVerificationCode(String toEmail, String username, String code) {
        String host = getOrDefault("MAIL_SMTP_HOST", "smtp.gmail.com");
        String port = getOrDefault("MAIL_SMTP_PORT", "587");
        String mailUser = required("MAIL_USERNAME");
        String mailPassword = required("MAIL_PASSWORD");
        String from = getOrDefault("MAIL_FROM", mailUser);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUser, mailPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Shadow Dimension - Email Verification");

            String safeName = (username == null || username.isBlank()) ? "Shadow Dweller" : username;
            String body = "Hello " + safeName + ",\n\n"
                    + "Your verification code is: " + code + "\n\n"
                    + "This code expires in 10 minutes.\n\n"
                    + "If you did not create this account, ignore this email.";

            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to send verification email: " + e.getMessage(), e);
        }
    }

    // Reads mandatory SMTP environment variables.
    private String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    // Reads optional SMTP values with a default fallback.
    private String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
