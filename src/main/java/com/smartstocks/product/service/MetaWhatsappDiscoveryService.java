package com.smartstocks.product.service;

import com.smartstocks.product.dto.DiscoveredWabaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MetaWhatsappDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsappDiscoveryService.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v25.0";
    private static final String DUMMY_WABA_ID = "1726866808739698";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${meta.oauth.client-id:}")
    private String metaClientId;

    @Value("${meta.oauth.client-secret:}")
    private String metaClientSecret;

    public String computeAppSecretProof(String accessToken) {
        if (metaClientSecret == null || metaClientSecret.isBlank() || accessToken == null || accessToken.isBlank()) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(metaClientSecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(accessToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] Failed to compute appsecret_proof: {}", ex.getMessage());
            return "";
        }
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String appendProof(String url, String accessToken) {
        String proof = computeAppSecretProof(accessToken);
        if (proof != null && !proof.isBlank()) {
            return url + (url.contains("?") ? "&" : "?") + "appsecret_proof=" + proof;
        }
        return url;
    }

    /**
     * Attempts to discover all accessible WABAs and Phone Numbers for the given token
     * across multiple Meta Graph API endpoints.
     */
    public List<DiscoveredWabaInfo> discoverAll(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Collections.emptyList();
        }
        log.info("[MetaDiscovery] Starting full WABA discovery for token (length={})...", accessToken.length());
        Map<String, DiscoveredWabaInfo> resultMap = new LinkedHashMap<>();

        // 0. Inspect token permissions (/me/permissions)
        inspectPermissions(accessToken);

        // 1. debug_token endpoint (extract granular scopes target_ids)
        discoverFromDebugToken(accessToken, resultMap);

        // 2. /me/whatsapp_business_accounts
        discoverFromMeWabas(accessToken, resultMap);

        // 3. /me/businesses -> owned and client WABAs
        discoverFromBusinesses(accessToken, resultMap);

        // 4. /me/accounts (Facebook Pages with linked WhatsApp)
        discoverFromPages(accessToken, resultMap);

        // 5. App-level WABAs if metaClientId is configured
        discoverFromApp(accessToken, resultMap);

        // For all discovered WABAs without phone numbers, attempt to fetch phone numbers
        for (Map.Entry<String, DiscoveredWabaInfo> entry : resultMap.entrySet()) {
            DiscoveredWabaInfo info = entry.getValue();
            if (info.getPhoneNumberId() == null || info.getPhoneNumberId().isBlank()) {
                enrichPhoneNumbersForWaba(accessToken, info);
            }
        }

        log.info("[MetaDiscovery] Discovery completed. Total discovered WABA accounts: {}", resultMap.size());
        resultMap.forEach((id, waba) -> log.info("[MetaDiscovery] -> WABA ID: {}, Name: {}, Phone ID: {}, Display: {}",
                id, waba.getWabaName(), waba.getPhoneNumberId(), waba.getDisplayPhoneNumber()));

        return new ArrayList<>(resultMap.values());
    }

    /**
     * Resolves the primary WABA and Phone Number for a campaign.
     */
    public Optional<DiscoveredWabaInfo> discoverPrimary(String accessToken, String knownPhoneNumberId, String knownWabaId) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }

        // If phone number ID is already provided, resolve its linked WABA directly
        if (knownPhoneNumberId != null && !knownPhoneNumberId.isBlank()) {
            Optional<DiscoveredWabaInfo> fromPhone = discoverFromPhoneNumberId(accessToken, knownPhoneNumberId.trim());
            if (fromPhone.isPresent() && !DUMMY_WABA_ID.equals(fromPhone.get().getWabaId())) {
                return fromPhone;
            }
        }

        // If WABA ID is known and valid (not dummy 1726866808739698), enrich it with phone numbers
        if (knownWabaId != null && !knownWabaId.isBlank() && !DUMMY_WABA_ID.equals(knownWabaId.trim())) {
            DiscoveredWabaInfo info = DiscoveredWabaInfo.builder()
                    .wabaId(knownWabaId.trim())
                    .build();
            enrichPhoneNumbersForWaba(accessToken, info);
            return Optional.of(info);
        }

        List<DiscoveredWabaInfo> all = discoverAll(accessToken);
        for (DiscoveredWabaInfo info : all) {
            if (!DUMMY_WABA_ID.equals(info.getWabaId())) {
                return Optional.of(info);
            }
        }
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /**
     * Inspects /me/permissions and logs granted/declined scopes.
     */
    private void inspectPermissions(String accessToken) {
        try {
            String url = appendProof(GRAPH_BASE + "/me/permissions", accessToken);
            ResponseEntity<Map> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Object data = resp.getBody().get("data");
                log.info("[MetaDiscovery] Granted Permissions for Token: {}", data);
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] /me/permissions check: {}", ex.getMessage());
        }
    }

    /**
     * Discovers WABA from a specific Phone Number ID.
     */
    public Optional<DiscoveredWabaInfo> discoverFromPhoneNumberId(String accessToken, String phoneNumberId) {
        if (phoneNumberId == null || phoneNumberId.isBlank() || phoneNumberId.startsWith("+")) {
            return Optional.empty();
        }
        try {
            String url = appendProof(GRAPH_BASE + "/" + phoneNumberId.trim() + "?fields=id,display_phone_number,verified_name,whatsapp_business_account", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<?, ?> body = resp.getBody();
                String displayPhone = (String) body.get("display_phone_number");
                String verifiedName = (String) body.get("verified_name");
                String wabaId = null;
                String wabaName = null;
                if (body.get("whatsapp_business_account") instanceof Map) {
                    Map<?, ?> wabaMap = (Map<?, ?>) body.get("whatsapp_business_account");
                    wabaId = String.valueOf(wabaMap.get("id"));
                    wabaName = (String) wabaMap.get("name");
                }
                if (wabaId != null && !wabaId.isBlank() && !"null".equals(wabaId)) {
                    log.info("[MetaDiscovery] Resolved WABA ID {} from phone number ID {}", wabaId, phoneNumberId);
                    return Optional.of(DiscoveredWabaInfo.builder()
                            .wabaId(wabaId)
                            .wabaName(wabaName != null ? wabaName : "WhatsApp Account (" + wabaId + ")")
                            .phoneNumberId(phoneNumberId)
                            .displayPhoneNumber(displayPhone)
                            .verifiedName(verifiedName)
                            .build());
                }
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] Failed to resolve WABA from phone number ID {}: {}", phoneNumberId, ex.getMessage());
        }
        return Optional.empty();
    }

    private void discoverFromDebugToken(String accessToken, Map<String, DiscoveredWabaInfo> resultMap) {
        try {
            String appToken = (metaClientId != null && !metaClientId.isBlank() && metaClientSecret != null && !metaClientSecret.isBlank())
                    ? metaClientId.trim() + "|" + metaClientSecret.trim()
                    : accessToken.trim();
            String url = GRAPH_BASE + "/debug_token?input_token="
                    + URLEncoder.encode(accessToken.trim(), StandardCharsets.UTF_8)
                    + "&access_token=" + URLEncoder.encode(appToken, StandardCharsets.UTF_8);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<?, ?> body = resp.getBody();
                log.info("[MetaDiscovery] debug_token response: {}", body);
                if (body.get("data") instanceof Map) {
                    Map<?, ?> data = (Map<?, ?>) body.get("data");
                    if (data.get("granular_scopes") instanceof List) {
                        List<?> granularScopes = (List<?>) data.get("granular_scopes");
                        List<String> wabaIds = new ArrayList<>();
                        List<String> phoneIds = new ArrayList<>();
                        List<String> businessIds = new ArrayList<>();

                        for (Object item : granularScopes) {
                            if (item instanceof Map) {
                                Map<?, ?> scopeMap = (Map<?, ?>) item;
                                String scope = (String) scopeMap.get("scope");
                                if (scopeMap.get("target_ids") instanceof List) {
                                    List<?> targets = (List<?>) scopeMap.get("target_ids");
                                    for (Object tid : targets) {
                                        if (tid != null) {
                                            String targetId = String.valueOf(tid);
                                            if ("whatsapp_business_management".equalsIgnoreCase(scope)) {
                                                wabaIds.add(targetId);
                                            } else if ("whatsapp_business_messaging".equalsIgnoreCase(scope)) {
                                                phoneIds.add(targetId);
                                            } else if ("business_management".equalsIgnoreCase(scope)) {
                                                businessIds.add(targetId);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (String wabaId : wabaIds) {
                            if (!resultMap.containsKey(wabaId) && !DUMMY_WABA_ID.equals(wabaId)) {
                                log.info("[MetaDiscovery] Discovered WABA ID {} from debug_token granular scopes", wabaId);
                                resultMap.put(wabaId, DiscoveredWabaInfo.builder()
                                        .wabaId(wabaId)
                                        .wabaName("WhatsApp Account (" + wabaId + ")")
                                        .build());
                            }
                        }

                        for (String phoneId : phoneIds) {
                            discoverFromPhoneNumberId(accessToken, phoneId).ifPresent(info -> {
                                resultMap.put(info.getWabaId(), info);
                            });
                        }

                        for (String bizId : businessIds) {
                            fetchWabasForBusiness(accessToken, bizId, "owned_whatsapp_business_accounts", resultMap);
                            fetchWabasForBusiness(accessToken, bizId, "client_whatsapp_business_accounts", resultMap);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] debug_token inspection notice: {}", ex.getMessage());
        }
    }

    private void discoverFromMeWabas(String accessToken, Map<String, DiscoveredWabaInfo> resultMap) {
        try {
            String url = appendProof(GRAPH_BASE + "/me/whatsapp_business_accounts?fields=id,name,timezone_id,currency", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                log.info("[MetaDiscovery] /me/whatsapp_business_accounts returned: {}", resp.getBody());
                extractWabaData(resp.getBody(), resultMap);
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] /me/whatsapp_business_accounts notice: {}", ex.getMessage());
        }
    }

    private void discoverFromBusinesses(String accessToken, Map<String, DiscoveredWabaInfo> resultMap) {
        try {
            String url = appendProof(GRAPH_BASE + "/me/businesses?fields=id,name", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<?, ?> body = resp.getBody();
                log.info("[MetaDiscovery] /me/businesses returned: {}", body);
                if (body.get("data") instanceof List) {
                    List<?> businesses = (List<?>) body.get("data");
                    for (Object bObj : businesses) {
                        if (bObj instanceof Map) {
                            String bizId = String.valueOf(((Map<?, ?>) bObj).get("id"));
                            fetchWabasForBusiness(accessToken, bizId, "owned_whatsapp_business_accounts", resultMap);
                            fetchWabasForBusiness(accessToken, bizId, "client_whatsapp_business_accounts", resultMap);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] /me/businesses notice: {}", ex.getMessage());
        }
    }

    private void fetchWabasForBusiness(String accessToken, String bizId, String edge, Map<String, DiscoveredWabaInfo> resultMap) {
        try {
            String url = appendProof(GRAPH_BASE + "/" + bizId + "/" + edge + "?fields=id,name,timezone_id,currency", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                log.info("[MetaDiscovery] Business {} edge {} returned: {}", bizId, edge, resp.getBody());
                extractWabaData(resp.getBody(), resultMap);
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] Business {} edge {} notice: {}", bizId, edge, ex.getMessage());
        }
    }

    private void discoverFromPages(String accessToken, Map<String, DiscoveredWabaInfo> resultMap) {
        try {
            String url = appendProof(GRAPH_BASE + "/me/accounts?fields=id,name,whatsapp_business_account", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<?, ?> body = resp.getBody();
                if (body.get("data") instanceof List) {
                    List<?> pages = (List<?>) body.get("data");
                    for (Object pObj : pages) {
                        if (pObj instanceof Map) {
                            Map<?, ?> pMap = (Map<?, ?>) pObj;
                            if (pMap.get("whatsapp_business_account") instanceof Map) {
                                Map<?, ?> wMap = (Map<?, ?>) pMap.get("whatsapp_business_account");
                                String wabaId = String.valueOf(wMap.get("id"));
                                if (wabaId != null && !wabaId.isBlank() && !"null".equals(wabaId) && !DUMMY_WABA_ID.equals(wabaId)) {
                                    log.info("[MetaDiscovery] Discovered WABA ID {} from Page: {}", wabaId, pMap.get("name"));
                                    String pageWabaName = wMap.get("name") != null ? String.valueOf(wMap.get("name")) : "Page WhatsApp (" + pMap.get("name") + ")";
                                    resultMap.put(wabaId, DiscoveredWabaInfo.builder()
                                            .wabaId(wabaId)
                                            .wabaName(pageWabaName)
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] /me/accounts notice: {}", ex.getMessage());
        }
    }

    private void discoverFromApp(String accessToken, Map<String, DiscoveredWabaInfo> resultMap) {
        if (metaClientId == null || metaClientId.isBlank()) {
            return;
        }
        try {
            String url = appendProof(GRAPH_BASE + "/" + metaClientId.trim() + "/whatsapp_business_accounts?fields=id,name,timezone_id,currency", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                log.info("[MetaDiscovery] App {} whatsapp_business_accounts returned: {}", metaClientId, resp.getBody());
                extractWabaData(resp.getBody(), resultMap);
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] App whatsapp_business_accounts notice: {}", ex.getMessage());
        }
    }

    private void extractWabaData(Map<?, ?> body, Map<String, DiscoveredWabaInfo> resultMap) {
        if (body.get("data") instanceof List) {
            List<?> list = (List<?>) body.get("data");
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    String wabaId = String.valueOf(map.get("id"));
                    String wabaName = (String) map.get("name");
                    String phoneId = null;
                    String displayPhone = null;
                    String verifiedName = null;

                    if (map.get("phone_numbers") instanceof Map) {
                        Map<?, ?> phonesObj = (Map<?, ?>) map.get("phone_numbers");
                        if (phonesObj.get("data") instanceof List) {
                            List<?> phoneList = (List<?>) phonesObj.get("data");
                            if (!phoneList.isEmpty() && phoneList.get(0) instanceof Map) {
                                Map<?, ?> pMap = (Map<?, ?>) phoneList.get(0);
                                phoneId = String.valueOf(pMap.get("id"));
                                displayPhone = (String) pMap.get("display_phone_number");
                                verifiedName = (String) pMap.get("verified_name");
                            }
                        }
                    }

                    if (wabaId != null && !wabaId.isBlank() && !"null".equals(wabaId) && !DUMMY_WABA_ID.equals(wabaId)) {
                        log.info("[MetaDiscovery] Extracted WABA ID: {}, Name: {}, Phone: {}", wabaId, wabaName, displayPhone);
                        resultMap.put(wabaId, DiscoveredWabaInfo.builder()
                                .wabaId(wabaId)
                                .wabaName(wabaName != null ? wabaName : "WhatsApp Account (" + wabaId + ")")
                                .phoneNumberId(phoneId)
                                .displayPhoneNumber(displayPhone)
                                .verifiedName(verifiedName)
                                .build());
                    }
                }
            }
        }
    }

    private void enrichPhoneNumbersForWaba(String accessToken, DiscoveredWabaInfo info) {
        if (info.getWabaId() == null || info.getWabaId().isBlank() || DUMMY_WABA_ID.equals(info.getWabaId())) return;
        try {
            String url = appendProof(GRAPH_BASE + "/" + info.getWabaId() + "/phone_numbers?fields=id,display_phone_number,verified_name", accessToken);
            URI uri = URI.create(url);
            ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<?, ?> body = resp.getBody();
                if (body.get("data") instanceof List) {
                    List<?> list = (List<?>) body.get("data");
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        Map<?, ?> pMap = (Map<?, ?>) list.get(0);
                        info.setPhoneNumberId(String.valueOf(pMap.get("id")));
                        info.setDisplayPhoneNumber((String) pMap.get("display_phone_number"));
                        info.setVerifiedName((String) pMap.get("verified_name"));
                        log.info("[MetaDiscovery] Enriched WABA {} with Phone Number ID {} ({})",
                                info.getWabaId(), info.getPhoneNumberId(), info.getDisplayPhoneNumber());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[MetaDiscovery] enrich phone numbers for WABA {} notice: {}", info.getWabaId(), ex.getMessage());
        }
    }
}
