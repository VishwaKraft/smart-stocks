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

    /** Soft-deletes the activity (isDeleted=true). */
    boolean deleteActivity(Long id);

    /**
     * Sets status = ACTIVE. Activity will be picked up by the scheduler on next tick.
     */
    CampaignActivityDto activateActivity(Long id);

    /**
     * Sets status = PAUSED. Scheduler will skip this activity until re-activated.
     */
    CampaignActivityDto pauseActivity(Long id);

    void testTrigger(Long id, java.util.List<String> emailIds);

    CampaignActivityDto cloneActivity(Long id, String newName);
}

