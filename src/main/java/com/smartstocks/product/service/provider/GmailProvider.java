package com.smartstocks.product.service.provider;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import java.util.List;

public class GmailProvider implements IEmailProvider {

    private final String accessToken;
    private final RestTemplate restTemplate;

    public GmailProvider(String accessToken) {
        this.accessToken = accessToken;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public SendResult send(RenderedTemplate rendered, List<String> recipients) {
        try {
            // 1. Create a raw RFC 2822 email message
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);

            email.setFrom(new InternetAddress("me"));
            for (String to : recipients) {
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
            }
            email.setSubject(rendered.getRenderedSubject());

            if (rendered.getRenderedBody() != null && !rendered.getRenderedBody().isEmpty()) {
                email.setContent(rendered.getRenderedBody(), "text/html; charset=utf-8");
            } else {
                email.setText("");
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawMessageBytes = buffer.toByteArray();
            String encodedEmail = Base64.getUrlEncoder().encodeToString(rawMessageBytes);

            // 2. Call Gmail API
            String url = "https://gmail.googleapis.com/upload/gmail/v1/users/me/messages/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(this.accessToken);

            Map<String, String> body = new HashMap<>();
            body.put("raw", encodedEmail);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return SendResult.ok(recipients.size(), "Message sent");
            } else {
                return SendResult.failure("Gmail API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return SendResult.failure("Error composing/sending email via Gmail: " + e.getMessage());
        }
    }
}
