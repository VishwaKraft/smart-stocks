package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mailgun provider stub.
 * Replace the log statement with actual Mailgun REST API calls.
 */
@Slf4j
@Component
public class MailgunProvider implements IEmailProvider {

    @Override
    public SendResult send(RenderedTemplate rendered, List<String> recipients) {
        log.info("[Mailgun] Sending to {} recipient(s). Subject: {}", recipients.size(), rendered.getRenderedSubject());
        // TODO: use RestTemplate or Mailgun SDK to POST to /v3/{domain}/messages
        return SendResult.ok(recipients.size(), "MAILGUN:SIMULATED_ID_" + System.currentTimeMillis());
    }
}
