package com.smartstocks.product.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappWebhookController.class);

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
     * Receives incoming WhatsApp messages and status updates.
     */
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Received WhatsApp webhook payload: {}", payload);
        // Process the incoming message or status update here
        // E.g. Update activity status, log message replies, etc.
        return ResponseEntity.ok().build();
    }
}
