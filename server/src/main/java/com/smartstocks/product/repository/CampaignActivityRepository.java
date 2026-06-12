package com.smartstocks.product.repository;

import com.smartstocks.product.models.ActivityStatus;
import com.smartstocks.product.models.CampaignActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignActivityRepository extends JpaRepository<CampaignActivity, Long> {

    List<CampaignActivity> findAllByCampaignId(Long campaignId);

    List<CampaignActivity> findAllByStatus(ActivityStatus status);

    /**
     * Fetch active activities whose next_execution_at has arrived.
     * Used by the scheduler every minute.
     */
    @Query("SELECT a FROM CampaignActivity a WHERE a.status = 'ACTIVE' AND a.nextExecutionAt <= :now")
    List<CampaignActivity> findDueActivities(@Param("now") LocalDateTime now);
}
