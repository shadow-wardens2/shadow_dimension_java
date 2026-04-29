package Services.User;

import Utils.AppConfig;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Sends one-time verification code using SMTP settings from environment variables.
    public void sendVerificationCode(String toEmail, String username, String code) {
        String safeName = (username == null || username.isBlank()) ? "Shadow Dweller" : username;
        sendEmail(
                toEmail,
                "Shadow Dimension - Email Verification",
                "Hello " + safeName + ",\n\n"
                        + "Your verification code is: " + code + "\n\n"
                        + "This code expires in 10 minutes.\n\n"
                        + "If you did not create this account, ignore this email."
        );
    }

    public void sendPasswordResetCode(String toEmail, String username, String code) {
        String safeName = (username == null || username.isBlank()) ? "Shadow Dweller" : username;
        sendEmail(
                toEmail,
                "Shadow Dimension - Password Reset",
                "Hello " + safeName + ",\n\n"
                        + "Your password reset code is: " + code + "\n\n"
                        + "This code expires in 10 minutes.\n\n"
                        + "If you did not request a password reset, ignore this email."
        );
    }

    private void sendEmail(String toEmail, String subject, String body) {
        String host = getOrDefault("MAIL_SMTP_HOST", "smtp.gmail.com");
        String port = getOrDefault("MAIL_SMTP_PORT", "587");
        String mailUser = required("MAIL_USERNAME").trim();
        // Google App Password is sometimes copied with spaces (e.g. "abcd efgh ijkl mnop").
        String mailPassword = required("MAIL_PASSWORD").replaceAll("\\s+", "");
        String from = getOrDefault("MAIL_FROM", mailUser).trim();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

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
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (AuthenticationFailedException e) {
            throw new IllegalStateException(
                    "SMTP authentication failed. Verify MAIL_USERNAME and MAIL_PASSWORD (Gmail App Password).",
                    e
            );
        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "SMTP send failed via " + host + ":" + port + ". Detail: " + e.getMessage(),
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to send verification email: " + e.getMessage(), e);
        }
    }

    // Reads mandatory SMTP environment variables.
    private String required(String key) {
        return AppConfig.required(key);
    }

    // Reads optional SMTP values with a default fallback.
    private String getOrDefault(String key, String defaultValue) {
        return AppConfig.getOrDefault(key, defaultValue);
    }
}
