package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.EmailOpenEventRepository;
import com.smartstocks.product.service.ICampaignService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class CampaignServiceImpl implements ICampaignService {

    private static final String PIXEL_PATH = "email_stocks.png";
    private static final int RANDOM_SUFFIX_LENGTH = 6;

    private final CampaignRepository campaignRepository;
    private final EmailOpenEventRepository emailOpenEventRepository;
    private final String trackingBaseUrl;

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

        Campaign saved = campaignRepository.save(campaign);
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
                    campaignRepository.delete(campaign);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public String buildTrackingPixelUrl(String campaignCode) {
        return UriComponentsBuilder.fromHttpUrl(trackingBaseUrl)
                .pathSegment(PIXEL_PATH)
                .queryParam("campaign", campaignCode)
                .build()
                .toUriString();
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
