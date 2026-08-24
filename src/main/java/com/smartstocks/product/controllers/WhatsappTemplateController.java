package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.DiscoveredWabaInfo;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.MetaWhatsappDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proxy controller that forwards WhatsApp template management requests to the Meta Graph API.
 * Includes automatic discovery of WhatsApp Business Accounts (WABA) and automatic retry on ID mismatch.
 */
@RestController
@RequestMapping("/api/whatsapp/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsappTemplateController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappTemplateController.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v25.0";
    private static final String DUMMY_WABA_ID = "1726866808739698";

    private final ICampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final MetaWhatsappDiscoveryService metaWhatsappDiscoveryService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${meta.waba-id:}")
    private String configuredWabaId;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getAccessToken(Long campaignId, String manualToken) {
        if (manualToken != null && !manualToken.isBlank()) {
            return manualToken.trim();
        }
        if (campaignId != null) {
            Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
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
     * Resolves the WABA ID to use. Priority:
     * 1. Explicit wabaId parameter (if valid and not dummy)
     * 2. Campaign's own metaWabaId (if valid and not dummy)
     * 3. Auto-discovery from token / phone number
     * 4. Global configured fallback
     */
    private String resolveWabaId(String wabaId, Long campaignId, String manualToken) {
        if (wabaId != null && !wabaId.isBlank() && !wabaId.startsWith("+") && !DUMMY_WABA_ID.equals(wabaId.trim())) {
            return wabaId.trim();
        }
        String token = null;
        try {
            token = getAccessToken(campaignId, manualToken);
        } catch (Exception ignored) {}

        if (campaignId != null) {
            Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
            if (campaignOpt.isPresent()) {
                Campaign campaign = campaignOpt.get();
                String campaignWabaId = campaign.getMetaWabaId();
                if (campaignWabaId != null && !campaignWabaId.isBlank() && !DUMMY_WABA_ID.equals(campaignWabaId.trim())) {
                    return campaignWabaId.trim();
                }
                // Try auto-discovering from the campaign's token and phone number ID
                if (token != null && !token.isBlank()) {
                    Optional<DiscoveredWabaInfo> discovered = metaWhatsappDiscoveryService.discoverPrimary(
                            token, campaign.getMetaPhoneNumberId(), null);
                    if (discovered.isPresent() && !DUMMY_WABA_ID.equals(discovered.get().getWabaId())) {
                        String discId = discovered.get().getWabaId();
                        campaign.setMetaWabaId(discId);
                        if (discovered.get().getPhoneNumberId() != null && (campaign.getMetaPhoneNumberId() == null || campaign.getMetaPhoneNumberId().isBlank())) {
                            campaign.setMetaPhoneNumberId(discovered.get().getPhoneNumberId());
                        }
                        campaignRepository.save(campaign);
                        log.info("[WhatsappTemplateController] Auto-discovered & persisted WABA ID {} for campaignId={}", discId, campaignId);
                        return discId;
                    }
                }
            }
        }

        if (token != null && !token.isBlank()) {
            Optional<DiscoveredWabaInfo> discovered = metaWhatsappDiscoveryService.discoverPrimary(token, null, null);
            if (discovered.isPresent() && !DUMMY_WABA_ID.equals(discovered.get().getWabaId())) {
                return discovered.get().getWabaId();
            }
        }

        if (configuredWabaId != null && !configuredWabaId.isBlank() && !DUMMY_WABA_ID.equals(configuredWabaId.trim())) {
            return configuredWabaId.trim();
        }

        return null;
    }

    /** Builds a full Meta Graph API URL for a WABA with appsecret_proof appended. */
    private String buildUrl(String wabaId, String path, String accessToken, String extraParams) {
        String proof = metaWhatsappDiscoveryService.computeAppSecretProof(accessToken);
        StringBuilder url = new StringBuilder(GRAPH_BASE)
                .append("/").append(wabaId.trim())
                .append("/").append(path);
        if (proof != null && !proof.isBlank()) {
            url.append("?appsecret_proof=").append(proof);
        }
        if (extraParams != null && !extraParams.isBlank()) {
            url.append(url.indexOf("?") >= 0 ? "&" : "?").append(extraParams);
        }
        return url.toString();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /**
     * Lists accessible WhatsApp Business Accounts for the specified campaign or manual token.
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken) {
        try {
            String token = getAccessToken(campaignId, manualToken);
            List<DiscoveredWabaInfo> accounts = metaWhatsappDiscoveryService.discoverAll(token);
            return ResponseEntity.ok(Map.of("data", accounts));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Error discovering WABA accounts: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getTemplates(
            @RequestParam(value = "wabaId", required = false) String wabaId,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken) {

        String token;
        try {
            token = getAccessToken(campaignId, manualToken);
        } catch (IllegalArgumentException ex) {
            log.warn("[WhatsappTemplateController] Invalid request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

        String resolvedWabaId = resolveWabaId(wabaId, campaignId, manualToken);
        if (resolvedWabaId == null || resolvedWabaId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No valid WhatsApp Business Account ID (WABA ID) found. Please select a campaign with WhatsApp credentials or enter your WABA ID manually."));
        }

        log.info("[WhatsappTemplateController] Fetching templates for wabaId={}, campaignId={}", resolvedWabaId, campaignId);
        try {
            String url = buildUrl(resolvedWabaId, "message_templates", token, null);
            log.debug("[WhatsappTemplateController] GET {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), Map.class);
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException ex) {
            log.warn("[WhatsappTemplateController] Meta API error fetching templates for wabaId={}: status={}, body={}",
                    resolvedWabaId, ex.getStatusCode(), ex.getResponseBodyAsString());

            // Handle invalid object ID / permission error (code 100 subcode 33) by auto-healing with discovered WABA
            String errorBody = ex.getResponseBodyAsString();
            if (errorBody.contains("100") || errorBody.contains("does not exist") || errorBody.contains("Unsupported get request")) {
                List<DiscoveredWabaInfo> discoveredList = metaWhatsappDiscoveryService.discoverAll(token);
                for (DiscoveredWabaInfo disc : discoveredList) {
                    if (disc.getWabaId() != null && !disc.getWabaId().equals(resolvedWabaId) && !DUMMY_WABA_ID.equals(disc.getWabaId())) {
                        log.info("[WhatsappTemplateController] Retrying template fetch with discovered WABA ID: {}", disc.getWabaId());
                        try {
                            String retryUrl = buildUrl(disc.getWabaId(), "message_templates", token, null);
                            ResponseEntity<Map> retryResp = restTemplate.exchange(
                                    retryUrl, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), Map.class);

                            if (campaignId != null) {
                                campaignRepository.findById(campaignId).ifPresent(c -> {
                                    c.setMetaWabaId(disc.getWabaId());
                                    if (disc.getPhoneNumberId() != null && (c.getMetaPhoneNumberId() == null || c.getMetaPhoneNumberId().isBlank())) {
                                        c.setMetaPhoneNumberId(disc.getPhoneNumberId());
                                    }
                                    campaignRepository.save(c);
                                });
                            }
                            return ResponseEntity.ok(retryResp.getBody());
                        } catch (Exception retryEx) {
                            log.warn("[WhatsappTemplateController] Retry with {} failed: {}", disc.getWabaId(), retryEx.getMessage());
                        }
                    }
                }
            }
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error fetching templates: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(
            @RequestParam(value = "wabaId", required = false) String wabaId,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken,
            @RequestBody Map<String, Object> payload) {

        String token;
        try {
            token = getAccessToken(campaignId, manualToken);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

        String resolvedWabaId = resolveWabaId(wabaId, campaignId, manualToken);
        if (resolvedWabaId == null || resolvedWabaId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No valid WhatsApp Business Account ID (WABA ID) found. Please specify your WABA ID."));
        }

        String templateName = payload != null ? String.valueOf(payload.get("name")) : "unknown";
        log.info("[WhatsappTemplateController] Creating template name={} for wabaId={}, campaignId={}",
                templateName, resolvedWabaId, campaignId);
        try {
            String url = buildUrl(resolvedWabaId, "message_templates", token, null);
            log.debug("[WhatsappTemplateController] POST {} — payload: {}", url, payload);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(payload, bearerHeaders(token)),
                    Map.class);
            log.info("[WhatsappTemplateController] Template created successfully: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappTemplateController] Meta API error creating template — status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error creating template: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteTemplate(
            @RequestParam(value = "wabaId", required = false) String wabaId,
            @RequestParam("name") String name,
            @RequestParam(value = "campaignId", required = false) Long campaignId,
            @RequestParam(value = "token", required = false) String manualToken) {

        String token;
        try {
            token = getAccessToken(campaignId, manualToken);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

        String resolvedWabaId = resolveWabaId(wabaId, campaignId, manualToken);
        if (resolvedWabaId == null || resolvedWabaId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No valid WhatsApp Business Account ID (WABA ID) found. Please specify your WABA ID."));
        }

        log.info("[WhatsappTemplateController] Deleting template name={} for wabaId={}, campaignId={}", name, resolvedWabaId, campaignId);
        try {
            String url = buildUrl(resolvedWabaId, "message_templates", token, "name=" + name);
            log.debug("[WhatsappTemplateController] DELETE {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(token)), Map.class);
            log.info("[WhatsappTemplateController] Template deleted: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException ex) {
            log.error("[WhatsappTemplateController] Meta API error deleting template — status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[WhatsappTemplateController] Unexpected error deleting template: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }
}
