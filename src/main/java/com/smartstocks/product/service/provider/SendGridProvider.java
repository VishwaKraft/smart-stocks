package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SendGrid provider stub.
 * Replace the log statement with actual SendGrid Java SDK calls.
 */
@Slf4j
@Component
public class SendGridProvider implements IEmailProvider {

    @Override
    public SendResult send(RenderedTemplate rendered, List<String> recipients) {
        log.info("[SendGrid] Sending to {} recipient(s). Subject: {}", recipients.size(), rendered.getRenderedSubject());
        // TODO: inject SendGrid client and call API with recipients, subject, htmlBody
        return SendResult.ok(recipients.size(), "SENDGRID:SIMULATED_MSG_ID_" + System.currentTimeMillis());
    }
}
