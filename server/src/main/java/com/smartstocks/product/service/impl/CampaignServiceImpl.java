package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.EmailOpenEventRepository;
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
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;

@Service
public class CampaignServiceImpl implements ICampaignService {

    private static final String PIXEL_PATH = "email_stocks.png";
    private static final int RANDOM_SUFFIX_LENGTH = 6;

    private final CampaignRepository campaignRepository;
    private final EmailOpenEventRepository emailOpenEventRepository;
    private final String trackingBaseUrl;

    @Autowired
    private CampaignEventLogger eventLogger;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    public CampaignServiceImpl(
            CampaignRepository campaignRepository,
            EmailOpenEventRepository emailOpenEventRepository,
            @Value("${app.tracking.base-url}") String trackingBaseUrl) {
        this.campaignRepository = campaignRepository;
        this.emailOpenEventRepository = emailOpenEventRepository;
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
        if (htmlBody == null || htmlBody.isBlank() || campaignCode == null || campaignCode.isBlank()) {
            return htmlBody;
        }
        String pixelUrl = buildTrackingPixelUrl(campaignCode, emailId, activityId);
        return appendPixelTag(htmlBody, pixelUrl);
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode, String emailId, Long activityId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trackingBaseUrl)
                .pathSegment(PIXEL_PATH)
                .queryParam("campaign", campaignCode);
        if (emailId != null && !emailId.isBlank()) {
            builder.queryParam("email_id", emailId);
        }
        if (activityId != null) {
            builder.queryParam("activity_id", activityId);
        }
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
        if (openCount == 0) {
            openCount = emailOpenEventRepository.countByCampaign(campaign.getCampaignCode());
        }

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
