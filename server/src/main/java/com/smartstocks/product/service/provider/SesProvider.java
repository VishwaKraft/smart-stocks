package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AWS SES provider stub.
 * Replace the log statement with actual SES SDK calls (e.g. SesClient.sendEmail).
 */
@Slf4j
@Component
public class SesProvider implements IEmailProvider {

    @Override
    public SendResult send(RenderedTemplate rendered, List<String> recipients) {
        log.info("[SES] Sending to {} recipient(s). Subject: {}", recipients.size(), rendered.getRenderedSubject());
        // TODO: inject SesClient and call sendEmail() with recipients, subject, body
        return SendResult.ok(recipients.size(), "SES:SIMULATED_MESSAGE_ID_" + System.currentTimeMillis());
    }
}
