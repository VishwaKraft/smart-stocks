package com.smartstocks.product.controllers;

import com.smartstocks.product.service.CampaignEventLogger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsappWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final CampaignEventLogger eventLogger;

    @Value("${whatsapp.webhook.verify-token}")
    private String verifyToken;

    /**
     * Handles webhook verification requests from Facebook.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if (mode != null && token != null) {
            if (mode.equals("subscribe") && token.equals(verifyToken)) {
                logger.info("WhatsApp webhook verified successfully!");
                return ResponseEntity.ok(challenge);
            } else {
                logger.warn("WhatsApp webhook verification failed. Expected token: '{}', Received token: '{}'", verifyToken, token);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token mismatch.");
            }
        }
        logger.warn("WhatsApp webhook verification failed. Missing mode or token. mode={}, token={}", mode, token);
        return ResponseEntity.badRequest().body("Missing 'hub.mode' or 'hub.verify_token'");
    }

    /**
     * Receives incoming WhatsApp messages and status updates from Meta.
     * Logs every payload to the campaign event log for audit and debugging.
     */
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Received WhatsApp webhook payload: {}", payload);

        // Determine event sub-type (message vs. status update)
        String eventSubType = "WHATSAPP_WEBHOOK_RECEIVED";
        try {
            Object entry = payload.get("entry");
            if (entry instanceof java.util.List && !((java.util.List<?>) entry).isEmpty()) {
                Object firstEntry = ((java.util.List<?>) entry).get(0);
                if (firstEntry instanceof Map) {
                    Object changes = ((Map<?, ?>) firstEntry).get("changes");
                    if (changes instanceof java.util.List && !((java.util.List<?>) changes).isEmpty()) {
                        Object firstChange = ((java.util.List<?>) changes).get(0);
                        if (firstChange instanceof Map) {
                            Object value = ((Map<?, ?>) firstChange).get("value");
                            if (value instanceof Map) {
                                if (((Map<?, ?>) value).containsKey("messages")) {
                                    eventSubType = "WHATSAPP_INBOUND_MESSAGE";
                                } else if (((Map<?, ?>) value).containsKey("statuses")) {
                                    eventSubType = "WHATSAPP_STATUS_UPDATE";
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not determine WhatsApp event subtype: {}", e.getMessage());
        }

        // Log to campaign event log (async, non-blocking)
        Map<String, Object> logInfo = new HashMap<>(payload);
        logInfo.put("source", "whatsapp_webhook");
        eventLogger.log(eventSubType, logInfo);

        // Always respond 200 OK immediately — Meta expects a fast acknowledgement
        return ResponseEntity.ok().build();
    }
}
