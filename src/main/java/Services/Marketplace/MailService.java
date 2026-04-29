package Services.Marketplace;

import Utils.EnvConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailService {

    public static void sendMail(String to, String subject, String body) {
        String username = EnvConfig.get("MAIL_USERNAME");
        String password = EnvConfig.get("MAIL_PASSWORD");
        if (password != null) password = password.replaceAll("\\s+", "");
        
        String host = EnvConfig.get("MAIL_SMTP_HOST", "smtp.gmail.com");
        String port = EnvConfig.get("MAIL_SMTP_PORT", "587");
        String from = EnvConfig.get("MAIL_FROM", username);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        final String finalUser = username;
        final String finalPass = password;

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalUser, finalPass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            System.out.println("Email sent successfully to " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
