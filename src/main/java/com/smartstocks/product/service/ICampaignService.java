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

    String buildTrackingPixelUrl(String campaignCode);
}
