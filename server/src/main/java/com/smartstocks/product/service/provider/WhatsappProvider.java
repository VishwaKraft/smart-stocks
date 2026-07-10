package com.smartstocks.product.service.provider;

import com.smartstocks.product.service.renderer.RenderedTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Sends WhatsApp template messages via the Meta WhatsApp Business Cloud API.
 * Uses a permanent access token obtained through the "Sign in with Meta" flow.
 *
 * API endpoint: POST https://graph.facebook.com/v25.0/{phoneNumberId}/messages
 */
public class WhatsappProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsappProvider.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v25.0";

    private final String accessToken;
    private final String phoneNumberId;
    private final String appSecret;
    private final RestTemplate restTemplate;

    public WhatsappProvider(String accessToken, String phoneNumberId, String appSecret) {
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.appSecret = appSecret;
        this.restTemplate = new RestTemplate();
    }

    private String computeAppSecretProof(String token) {
        if (appSecret == null || appSecret.isBlank()) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("[WhatsappProvider] Failed to compute appsecret_proof", ex);
            return "";
        }
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
        String proof = computeAppSecretProof(accessToken);
        if (proof != null && !proof.isEmpty()) {
            url += "?appsecret_proof=" + proof;
        }

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
                String msgId = response.getBody() != null
                        ? String.valueOf(response.getBody().getOrDefault("messages", "unknown"))
                        : "unknown";
                return SendResult.ok(1, "WhatsApp message sent – id=" + msgId);
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
