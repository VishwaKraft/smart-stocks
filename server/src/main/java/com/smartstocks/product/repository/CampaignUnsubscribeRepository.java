package com.smartstocks.product.repository;

import com.smartstocks.product.models.CampaignUnsubscribe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CampaignUnsubscribeRepository extends JpaRepository<CampaignUnsubscribe, Long> {

    /** True if the email has already unsubscribed from this campaign. */
    boolean existsByCampaignIdAndEmailId(Long campaignId, String emailId);

    /** Retrieve all unsubscribed email addresses for a campaign as a set (used for filtering). */
    @Query("SELECT u.emailId FROM CampaignUnsubscribe u WHERE u.campaign.id = :campaignId")
    Set<String> findEmailIdsByCampaignId(@Param("campaignId") Long campaignId);

    /** All unsubscribe records for a campaign (admin/reporting use). */
    List<CampaignUnsubscribe> findByCampaignId(Long campaignId);

    /** Count of unsubscribes for a campaign. */
    long countByCampaignId(Long campaignId);
}
