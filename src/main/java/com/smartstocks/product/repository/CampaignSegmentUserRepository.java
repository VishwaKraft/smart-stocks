package com.smartstocks.product.repository;

import com.smartstocks.product.models.CampaignSegmentUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignSegmentUserRepository extends JpaRepository<CampaignSegmentUser, Long> {

    /** Find all pre-generated recipient records for a given campaign activity. */
    List<CampaignSegmentUser> findByActivityId(Long activityId);

    /** Count recipients for an activity. */
    long countByActivityId(Long activityId);

    /** Delete all recipient records for an activity (used when re-generating). */
    void deleteAllByActivityId(Long activityId);

    /** Count the total generated recipients for EMAIL campaigns across the whole system. */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM CampaignSegmentUser c WHERE c.activity.campaign.campaignType = 'EMAIL'")
    long countTotalEmailSends();
}
