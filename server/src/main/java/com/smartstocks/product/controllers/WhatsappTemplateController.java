package com.smartstocks.product.controllers;

import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.service.ICampaignService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Proxy controller that forwards WhatsApp template management requests to the Meta Graph API.
 *
 * All server-side Meta API calls must include an `appsecret_proof` parameter:
 *   appsecret_proof = HMAC-SHA256(app_secret, access_token)
 *
 * This prevents token theft attacks when making Graph API calls from a server.
 * See: https://developers.facebook.com/docs/graph-api/securing-requests
 */
@RestController
@RequestMapping("/api/whatsapp/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsappTemplateController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappTemplateController.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v25.0";

    private final ICampaignService campaignService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${meta.oauth.client-secret:}")
    private String appSecret;

    @Value("${meta.waba-id:1726866808739698}")
    private String configuredWabaId;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getAccessToken(Long campaignId, String manualToken) {
        if (manualToken != null && !manualToken.isBlank()) {
            return manualToken.trim();
        }
        if (campaignId != null) {
            Optional<Campaign> campaignOpt = campaignService.findById(campaignId);
            if (campaignOpt.isPresent()) {
                String token = campaignOpt.get().getMetaAccessToken();
                if (token != null && !token.isBlank()) {
                    return token.trim();
                }
            }
        }
        throw new IllegalArgumentException(
                "No valid Meta access token found. Please select a campaign with a saved token or enter a manual token.");
    }

    /**
     * Computes the appsecret_proof required for all server-side Meta Graph API calls.
     * Formula: HMAC-SHA256(key=app_secret, message=access_token), hex-encoded.
     */
    private String computeAppSecretProof(String accessToken) {
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("[WhatsappTemplateController] meta.oauth.client-secret is not configured — appsecret_proof will be missing.");
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Failed to compute appsecret_proof: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to compute appsecret_proof", ex);
        }
    }

    /** Builds a full Meta Graph API URL for a WABA with appsecret_proof appended. */
    private String buildUrl(String wabaId, String path, String accessToken, String extraParams) {
        String proof = computeAppSecretProof(accessToken);
        StringBuilder url = new StringBuilder(GRAPH_BASE)
                .append("/").append(wabaId)
                .append("/").append(path)
                .append("?appsecret_proof=").append(proof);
        if (extraParams != null && !extraParams.isBlank()) {
            url.append("&").append(extraParams);
        }
        return url.toString();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<?> getTemplates(
            @RequestParam("wabaId") String wabaId,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken) {

        if (wabaId == null || wabaId.isBlank() || wabaId.startsWith("+")) {
            wabaId = configuredWabaId;
        }
        log.info("[WhatsappTemplateController] Fetching templates for wabaId={}, campaignId={}", wabaId, campaignId);
        try {
            String token = getAccessToken(campaignId, manualToken);
            String url = buildUrl(wabaId, "message_templates", token, null);
            log.debug("[WhatsappTemplateController] GET {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), Map.class);
            return ResponseEntity.ok(response.getBody());

        } catch (IllegalArgumentException ex) {
            log.warn("[WhatsappTemplateController] Invalid request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappTemplateController] Meta API error fetching templates — status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error fetching templates: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(
            @RequestParam("wabaId") String wabaId,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken,
            @RequestBody Map<String, Object> payload) {

        String templateName = payload != null ? String.valueOf(payload.get("name")) : "unknown";
        if (wabaId == null || wabaId.isBlank() || wabaId.startsWith("+")) {
            wabaId = configuredWabaId;
        }
        log.info("[WhatsappTemplateController] Creating template name={} for wabaId={}, campaignId={}",
                templateName, wabaId, campaignId);
        try {
            String token = getAccessToken(campaignId, manualToken);
            String url = buildUrl(wabaId, "message_templates", token, null);
            log.debug("[WhatsappTemplateController] POST {} — payload: {}", url, payload);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(payload, bearerHeaders(token)),
                    Map.class);
            log.info("[WhatsappTemplateController] Template created successfully: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (IllegalArgumentException ex) {
            log.warn("[WhatsappTemplateController] Invalid request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappTemplateController] Meta API error creating template — status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error creating template: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteTemplate(
            @RequestParam("wabaId") String wabaId,
            @RequestParam("name") String name,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken) {

        if (wabaId == null || wabaId.isBlank() || wabaId.startsWith("+")) {
            wabaId = configuredWabaId;
        }
        log.info("[WhatsappTemplateController] Deleting template name={} for wabaId={}, campaignId={}", name, wabaId, campaignId);
        try {
            String token = getAccessToken(campaignId, manualToken);
            String url = buildUrl(wabaId, "message_templates", token, "name=" + name);
            log.debug("[WhatsappTemplateController] DELETE {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(token)), Map.class);
            log.info("[WhatsappTemplateController] Template deleted: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (IllegalArgumentException ex) {
            log.warn("[WhatsappTemplateController] Invalid request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappTemplateController] Meta API error deleting template — status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error deleting template: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}
