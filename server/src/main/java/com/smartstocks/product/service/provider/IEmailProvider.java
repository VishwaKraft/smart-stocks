package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;

import java.util.List;

/**
 * Strategy interface for email delivery providers.
 * Each implementation wraps a different sending infrastructure (SES, SendGrid, etc.)
 */
public interface IEmailProvider {

    /**
     * Send the rendered email to the given list of recipients.
     *
     * @param rendered   fully resolved subject and body
     * @param recipients list of recipient email addresses
     * @return SendResult with delivery outcome details
     */
    SendResult send(RenderedTemplate rendered, List<String> recipients);
}
