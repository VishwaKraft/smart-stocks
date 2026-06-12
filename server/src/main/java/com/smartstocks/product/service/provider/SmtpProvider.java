package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SMTP provider using javax.mail (JavaMail).
 *
 * Configure host/port/auth via application properties:
 *   app.smtp.host, app.smtp.port, app.smtp.username, app.smtp.password, app.smtp.from
 */
@Slf4j
@Component
public class SmtpProvider implements IEmailProvider {

    @Override
    public SendResult send(RenderedTemplate rendered, List<String> recipients) {
        log.info("[SMTP] Sending to {} recipient(s). Subject: {}", recipients.size(), rendered.getRenderedSubject());
        // TODO: inject @Value smtp properties and create Session + MimeMessage
        // Example skeleton below (not activated without actual config):
        //
        // Properties props = new Properties();
        // props.put("mail.smtp.host", smtpHost);
        // props.put("mail.smtp.port", smtpPort);
        // props.put("mail.smtp.auth", "true");
        // props.put("mail.smtp.starttls.enable", "true");
        //
        // Session session = Session.getInstance(props, new Authenticator() {
        //     protected PasswordAuthentication getPasswordAuthentication() {
        //         return new PasswordAuthentication(username, password);
        //     }
        // });
        //
        // for (String recipient : recipients) {
        //     MimeMessage message = new MimeMessage(session);
        //     message.setFrom(new InternetAddress(fromAddress));
        //     message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        //     message.setSubject(rendered.getRenderedSubject());
        //     message.setContent(rendered.getRenderedBody(), "text/html; charset=utf-8");
        //     Transport.send(message);
        // }
        return SendResult.ok(recipients.size(), "SMTP:SIMULATED_" + System.currentTimeMillis());
    }
}
