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

    /** Returns all non-soft-deleted activities for a campaign. */
    List<CampaignActivity> findAllByCampaignIdAndIsDeletedFalse(Long campaignId);

    /** Returns all non-soft-deleted activities (used by list API). */
    List<CampaignActivity> findAllByIsDeletedFalse();

    List<CampaignActivity> findAllByStatus(ActivityStatus status);

    /**
     * Fetch READY/ACTIVE activities that are due and NOT soft-deleted.
     * Used by the scheduler every minute.
     */
    @Query("SELECT a FROM CampaignActivity a " +
           "WHERE a.status IN ('ACTIVE', 'READY') " +
           "AND a.isDeleted = false " +
           "AND a.nextExecutionAt <= :now")
    List<CampaignActivity> findDueActivities(@Param("now") LocalDateTime now);

    boolean existsByActivityName(String activityName);

    boolean existsByActivityNameAndIdNot(String activityName, Long id);
}
