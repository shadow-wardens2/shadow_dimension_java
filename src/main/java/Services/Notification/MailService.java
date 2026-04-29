package Services.Notification;

import Utils.EnvConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class MailService {

    private final String username;
    private final String password;
    private final String fromEmail;
    private final String host;
    private final String port;

    public MailService() {
        this.username = EnvConfig.get("MAIL_USERNAME");
        this.password = EnvConfig.get("MAIL_PASSWORD");
        this.fromEmail = EnvConfig.get("MAIL_FROM");
        this.host = EnvConfig.get("MAIL_SMTP_HOST", "smtp.gmail.com");
        this.port = EnvConfig.get("MAIL_SMTP_PORT", "587");
    }

    public void sendEmail(String to, String subject, String content) throws MessagingException {
        if (username == null || password == null) {
            throw new MessagingException("Mail configuration is missing in .env file (MAIL_USERNAME/MAIL_PASSWORD)");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail != null ? fromEmail : username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
    }
    
    /**
     * Sends an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        if (username == null || password == null) {
            throw new MessagingException("Mail configuration is missing in .env file (MAIL_USERNAME/MAIL_PASSWORD)");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail != null ? fromEmail : username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
    }
}
