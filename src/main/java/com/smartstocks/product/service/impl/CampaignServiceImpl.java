package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.EmailOpenEventRepository;
import com.smartstocks.product.repository.WhatsappMessageLogRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ICampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class CampaignServiceImpl implements ICampaignService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CampaignServiceImpl.class);

    private static final String PIXEL_PATH = "email_stocks.png";
    private static final int RANDOM_SUFFIX_LENGTH = 6;

    private final CampaignRepository campaignRepository;
    private final EmailOpenEventRepository emailOpenEventRepository;
    private final WhatsappMessageLogRepository whatsappMessageLogRepository;
    private final String trackingBaseUrl;

    @Autowired
    private CampaignEventLogger eventLogger;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${meta.oauth.client-id:}")
    private String metaClientId;

    @Value("${meta.oauth.client-secret:}")
    private String metaClientSecret;

    @Value("${meta.waba-id:1726866808739698}")
    private String configuredWabaId;

    public CampaignServiceImpl(
            CampaignRepository campaignRepository,
            EmailOpenEventRepository emailOpenEventRepository,
            WhatsappMessageLogRepository whatsappMessageLogRepository,
            @Value("${app.tracking.base-url}") String trackingBaseUrl) {
        this.campaignRepository = campaignRepository;
        this.emailOpenEventRepository = emailOpenEventRepository;
        this.whatsappMessageLogRepository = whatsappMessageLogRepository;
        this.trackingBaseUrl = normalizeBaseUrl(trackingBaseUrl);
    }

    @Override
    @Transactional
    public CampaignDto createCampaign(CreateCampaignRequestDto request) {
        String campaignCode = resolveCampaignCode(request.getName(), request.getCampaignCode());

        Campaign campaign = new Campaign();
        campaign.setCampaignCode(campaignCode);
        campaign.setName(request.getName().trim());
        campaign.setDescription(trimToNull(request.getDescription()));

        if (request.getCampaignType() != null) {
            campaign.setCampaignType(request.getCampaignType());
        }

        if (request.getWhatsappSenderNumber() != null && !request.getWhatsappSenderNumber().isBlank()) {
            campaign.setWhatsappSenderNumber(request.getWhatsappSenderNumber());
        }

        if (request.getEmailProviderType() != null) {
            campaign.setEmailProviderType(request.getEmailProviderType());
        }

        Campaign saved = campaignRepository.save(campaign);

        Map<String, Object> info = new HashMap<>();
        info.put("campaignId", saved.getId());
        info.put("campaignName", saved.getName());
        info.put("provider", saved.getEmailProviderType());
        eventLogger.log("CAMPAIGN_CREATED", info);

        return toDto(saved);
    }

    @Override
    public List<CampaignDto> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CampaignDto> getCampaignById(Long id) {
        return campaignRepository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<Campaign> findByCampaignCode(String campaignCode) {
        if (campaignCode == null || campaignCode.isBlank()) {
            return Optional.empty();
        }
        return campaignRepository.findByCampaignCode(campaignCode.trim());
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return campaignRepository.findById(id);
    }

    @Override
    @Transactional
    public boolean deleteCampaign(Long id) {
        return campaignRepository.findById(id)
                .map(campaign -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("campaignId", campaign.getId());
                    info.put("campaignName", campaign.getName());
                    eventLogger.log("CAMPAIGN_DELETED", info);
                    campaignRepository.delete(campaign);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void saveGoogleToken(Long id, String accessToken) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        campaign.setGoogleAccessToken(accessToken);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void saveMetaToken(Long id, String accessToken, String phoneNumberId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        campaign.setMetaAccessToken(accessToken);
        campaign.setMetaPhoneNumberId(phoneNumberId);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void saveMetaAuthCode(Long id, String code, String redirectUri) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v25.0/oauth/access_token")
                .queryParam("client_id", metaClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_secret", metaClientSecret)
                .queryParam("code", code)
                .build().toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                campaign.setMetaAccessToken(accessToken);
                
                // Automatically fetch and store the Phone Number ID
                String appSecretProof = computeAppSecretProof(accessToken, metaClientSecret);
                String phoneNumbersUrl = "https://graph.facebook.com/v25.0/" + configuredWabaId + "/phone_numbers?access_token=" + accessToken + "&appsecret_proof=" + appSecretProof;
                
                try {
                    ResponseEntity<Map> phoneResponse = restTemplate.getForEntity(phoneNumbersUrl, Map.class);
                    if (phoneResponse.getStatusCode() == HttpStatus.OK && phoneResponse.getBody() != null) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) phoneResponse.getBody().get("data");
                        if (data != null && !data.isEmpty()) {
                            String phoneNumberId = String.valueOf(data.get(0).get("id"));
                            campaign.setMetaPhoneNumberId(phoneNumberId);
                            log.info("[CampaignService] Fetched and saved Meta Phone Number ID: {}", phoneNumberId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[CampaignService] Failed to fetch phone number ID: {}", e.getMessage());
                }

                campaignRepository.save(campaign);
            } else {
                throw new RuntimeException("Failed to exchange auth code for Meta access token: status=" + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("[CampaignService] Meta OAuth token exchange failed. Status: {}, Response: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Failed to exchange Meta auth code: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("[CampaignService] Unexpected error during Meta OAuth token exchange: {}", ex.getMessage(), ex);
            throw new RuntimeException("Unexpected error exchanging Meta auth code: " + ex.getMessage(), ex);
        }
    }

    private String computeAppSecretProof(String accessToken, String appSecret) {
        if (appSecret == null || appSecret.isBlank()) {
            return "";
        }
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(appSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(accessToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("[CampaignService] Failed to compute appsecret_proof: {}", ex.getMessage(), ex);
            return "";
        }
    }

    @Override
    @Transactional
    public void saveGoogleAuthCode(Long id, String code, String redirectUri) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", googleClientId);
        map.add("client_secret", googleClientSecret);
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String accessToken = (String) response.getBody().get("access_token");
            String refreshToken = (String) response.getBody().get("refresh_token");
            campaign.setGoogleAccessToken(accessToken);
            if (refreshToken != null) {
                campaign.setGoogleRefreshToken(refreshToken);
            }
            campaignRepository.save(campaign);
        } else {
            throw new RuntimeException("Failed to exchange auth code for Google tokens");
        }
    }

    @Override
    @Transactional
    public String refreshGoogleAccessToken(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (campaign.getGoogleRefreshToken() == null || campaign.getGoogleRefreshToken().isEmpty()) {
            throw new IllegalStateException("No refresh token available to refresh access token");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", googleClientId);
        map.add("client_secret", googleClientSecret);
        map.add("refresh_token", campaign.getGoogleRefreshToken());
        map.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String newAccessToken = (String) response.getBody().get("access_token");
            campaign.setGoogleAccessToken(newAccessToken);
            campaignRepository.save(campaign);
            return newAccessToken;
        } else {
            throw new RuntimeException("Failed to refresh Google access token");
        }
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode) {
        return buildTrackingPixelUrl(campaignCode, false);
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode, boolean isTest) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trackingBaseUrl)
                .pathSegment(PIXEL_PATH)
                .queryParam("campaign", campaignCode);
        
        if (isTest) {
            builder.queryParam("isTest", "true");
        }
        
        return builder.build().toUriString();
    }

    @Override
    public String injectTrackingPixel(String htmlBody, String campaignCode) {
        return injectTrackingPixel(htmlBody, campaignCode, false);
    }

    @Override
    public String injectTrackingPixel(String htmlBody, String campaignCode, boolean isTest) {
        if (htmlBody == null || htmlBody.isBlank() || campaignCode == null || campaignCode.isBlank()) {
            return htmlBody;
        }

        String pixelUrl = buildTrackingPixelUrl(campaignCode, isTest);
        return appendPixelTag(htmlBody, pixelUrl);
    }

    @Override
    public String injectTrackingPixel(String htmlBody, String campaignCode, String emailId, Long activityId) {
        return injectTrackingPixel(htmlBody, campaignCode, emailId, activityId, null);
    }

    @Override
    public String injectTrackingPixel(String htmlBody, String campaignCode, String emailId, Long activityId, String nonce) {
        if (htmlBody == null || htmlBody.isBlank() || campaignCode == null || campaignCode.isBlank()) {
            return htmlBody;
        }
        String pixelUrl = buildTrackingPixelUrl(campaignCode, emailId, activityId, nonce);
        return appendPixelTag(htmlBody, pixelUrl);
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode, String emailId, Long activityId) {
        return buildTrackingPixelUrl(campaignCode, emailId, activityId, null);
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode, String emailId, Long activityId, String nonce) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trackingBaseUrl)
                .pathSegment(PIXEL_PATH)
                .queryParam("campaign", campaignCode);
        if (emailId != null && !emailId.isBlank()) {
            builder.queryParam("email_id", emailId);
        }
        if (activityId != null) {
            builder.queryParam("activity_id", activityId);
        }
        // Append a unique nonce so that every recipient URL is distinct.
        // This prevents caching proxies (Gmail Image Proxy, Apple Mail, Yahoo)
        // from serving a single cached response for all recipients.
        String resolvedNonce = (nonce != null && !nonce.isBlank()) ? nonce : UUID.randomUUID().toString();
        builder.queryParam("nonce", resolvedNonce);
        // Secondary cache-buster: current epoch-millis makes the URL unique in time as well
        builder.queryParam("_t", System.currentTimeMillis());
        return builder.build().toUriString();
    }

    private String appendPixelTag(String htmlBody, String pixelUrl) {
        String pixelTag = String.format(
                "<img src=\"%s\" width=\"1\" height=\"1\" alt=\"\" style=\"display:none;\" />",
                pixelUrl);
        String lower = htmlBody.toLowerCase(Locale.ROOT);
        int bodyClose = lower.lastIndexOf("</body>");
        if (bodyClose >= 0) {
            return htmlBody.substring(0, bodyClose) + pixelTag + "\n" + htmlBody.substring(bodyClose);
        }
        return htmlBody + "\n" + pixelTag;
    }

    private CampaignDto toDto(Campaign campaign) {
        long openCount = emailOpenEventRepository.countByCampaignId(campaign.getId());
        if (openCount == 0 && campaign.getCampaignCode() != null) {
            openCount = emailOpenEventRepository.countByCampaign(campaign.getCampaignCode());
        }

        // Add WhatsApp Opens
        long whatsappOpenCount = whatsappMessageLogRepository.countByCampaignIdAndStatus(campaign.getId(), "read");
        openCount += whatsappOpenCount;

        return CampaignDto.builder()
                .id(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .campaignType(campaign.getCampaignType())
                .whatsappSenderNumber(campaign.getWhatsappSenderNumber())
                .emailProviderType(campaign.getEmailProviderType())
                .trackingPixelUrl(buildTrackingPixelUrl(campaign.getCampaignCode()))
                .openCount(openCount)
                .createdAt(campaign.getCreatedAt())
                .metaPhoneNumberId(campaign.getMetaPhoneNumberId())
                .wabaId(campaign.getWhatsappSenderNumber())  // whatsappSenderNumber stores WABA-ID for WA campaigns
                .build();
    }

    private String resolveCampaignCode(String name, String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String normalized = normalizeCampaignCode(requestedCode);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Invalid campaign code");
            }
            if (campaignRepository.existsByCampaignCode(normalized)) {
                throw new IllegalArgumentException("Campaign code already exists");
            }
            return normalized;
        }

        String base = slugify(name);
        if (base.isEmpty()) {
            base = "campaign";
        }

        String candidate = base;
        int attempt = 0;
        while (campaignRepository.existsByCampaignCode(candidate)) {
            attempt++;
            candidate = base + "-" + randomSuffix();
            if (attempt > 20) {
                throw new IllegalStateException("Unable to generate unique campaign code");
            }
        }
        return candidate;
    }

    private String normalizeCampaignCode(String code) {
        return slugify(code);
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }
        String slug = input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > 58) {
            slug = slug.substring(0, 58).replaceAll("-+$", "");
        }
        return slug;
    }

    private String randomSuffix() {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(RANDOM_SUFFIX_LENGTH);
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/tracking")) {
            return normalized;
        }
        if (!normalized.contains("/tracking")) {
            return normalized + "/tracking";
        }
        return normalized;
    }
}
