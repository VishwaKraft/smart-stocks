package com.smartstocks.product.service;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.models.Campaign;

import java.util.List;
import java.util.Optional;

public interface ICampaignService {

    CampaignDto createCampaign(CreateCampaignRequestDto request);

    List<CampaignDto> getAllCampaigns();

    Optional<CampaignDto> getCampaignById(Long id);

    Optional<Campaign> findByCampaignCode(String campaignCode);

    Optional<Campaign> findById(Long id);

    boolean deleteCampaign(Long id);

    void saveGoogleToken(Long id, String accessToken);

    void saveGoogleAuthCode(Long id, String code, String redirectUri);

    String refreshGoogleAccessToken(Long id);

    String buildTrackingPixelUrl(String campaignCode);

    String buildTrackingPixelUrl(String campaignCode, boolean isTest);

    /**
     * Appends a 1×1 tracking pixel to the HTML body (before {@code </body>} when present).
     */
    String injectTrackingPixel(String htmlBody, String campaignCode);

    String injectTrackingPixel(String htmlBody, String campaignCode, boolean isTest);

    /**
     * Injects a tracking pixel enriched with the recipient email and activity ID.
     * Used by the scheduler for per-recipient personalized tracking.
     */
    String injectTrackingPixel(String htmlBody, String campaignCode, String emailId, Long activityId);

    /** Builds a tracking pixel URL with optional emailId and activityId params. */
    String buildTrackingPixelUrl(String campaignCode, String emailId, Long activityId);
}
