package com.smartstocks.product.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends WhatsApp template messages via the Meta WhatsApp Business Cloud API.
 * Uses a permanent access token obtained through the "Sign in with Meta" flow.
 *
 * API endpoint: POST https://graph.facebook.com/v25.0/{phoneNumberId}/messages
 *
 * Note: appsecret_proof is NOT used here. The WhatsApp Cloud API /messages endpoint
 * only accepts Bearer token auth and rejects appsecret_proof query params with a 400
 * GraphMethodException ("Unsupported request - method type: post").
 */
public class WhatsappProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsappProvider.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v25.0";

    private final String accessToken;
    private final String phoneNumberId;
    private final RestTemplate restTemplate;

    public WhatsappProvider(String accessToken, String phoneNumberId, String appSecret) {
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        // appSecret is intentionally not used: the WhatsApp Cloud API /messages endpoint
        // does not support appsecret_proof and rejects requests that include it.
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a WhatsApp template message to the given phone number.
     *
     * @param toPhoneNumber  Recipient phone number in E.164 format (e.g. "+919258844009")
     * @param templateName   Meta-approved template name (e.g. "3p_direct_integration_test_template")
     * @param languageCode   Template language code (e.g. "en_US")
     * @return SendResult with success/failure details
     */
    public SendResult send(String toPhoneNumber, String templateName, String languageCode) {
        String url = GRAPH_API_BASE + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Build the WhatsApp Cloud API payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toPhoneNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        Map<String, String> language = new HashMap<>();
        language.put("code", languageCode != null ? languageCode : "en_US");
        template.put("language", language);
        payload.put("template", template);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        log.info("[WhatsappProvider] REQUEST url={} to={} template={}", url, toPhoneNumber, templateName);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            log.info("[WhatsappProvider] RESPONSE status={} body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                String msgId = "unknown";
                if (response.getBody() != null && response.getBody().get("messages") instanceof List) {
                    List<?> messages = (List<?>) response.getBody().get("messages");
                    if (!messages.isEmpty() && messages.get(0) instanceof Map) {
                        msgId = String.valueOf(((Map<?, ?>) messages.get(0)).get("id"));
                    }
                }
                return SendResult.ok(1, msgId);
            } else {
                return SendResult.failure("WhatsApp API error: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappProvider] RESPONSE error status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return SendResult.failureWithAuthError("Meta token expired or invalid: " + ex.getResponseBodyAsString());
            }
            return SendResult.failure("WhatsApp API error: " + ex.getStatusCode() + " – " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappProvider] Unexpected error: {}", ex.getMessage(), ex);
            return SendResult.failure("Error sending WhatsApp message: " + ex.getMessage());
        }
    }
}
