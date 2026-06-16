package com.smartstocks.product.service;

import com.smartstocks.product.dto.CampaignActivityDto;
import com.smartstocks.product.dto.CreateActivityRequestDto;

import java.util.List;
import java.util.Optional;

public interface ICampaignActivityService {

    CampaignActivityDto createActivity(CreateActivityRequestDto request);

    List<CampaignActivityDto> getAllActivities();

    List<CampaignActivityDto> getActivitiesByCampaign(Long campaignId);

    Optional<CampaignActivityDto> getActivityById(Long id);

    CampaignActivityDto updateActivity(Long id, CreateActivityRequestDto request);

    boolean deleteActivity(Long id);

    void testTrigger(Long id);
}
