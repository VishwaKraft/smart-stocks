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

    /**
     * Find RECURRING parent activities (parentActivity IS NULL) that are ACTIVE
     * and whose nextExecutionAt is due now. Used by Tick-A to spawn child occurrences.
     */
    @Query("SELECT a FROM CampaignActivity a " +
           "WHERE a.scheduleType = 'RECURRING' " +
           "AND a.parentActivity IS NULL " +
           "AND a.status IN ('ACTIVE', 'READY') " +
           "AND a.isDeleted = false " +
           "AND a.nextExecutionAt <= :now")
    List<CampaignActivity> findDueRecurringParents(@Param("now") LocalDateTime now);

    /**
     * Find child activities (parentActivity IS NOT NULL) in GENERATING status
     * whose executionDatetime is at or before (now + 5 min).
     * Used by Tick-B to auto-generate data ~5 min before send.
     */
    @Query("SELECT a FROM CampaignActivity a " +
           "WHERE a.parentActivity IS NOT NULL " +
           "AND a.status = 'GENERATING' " +
           "AND a.isDeleted = false " +
           "AND a.executionDatetime <= :threshold")
    List<CampaignActivity> findChildrenDueForGeneration(@Param("threshold") LocalDateTime threshold);

    /**
     * Fetch READY/ACTIVE ONE_TIME activities (children + plain one-time)
     * that are due and NOT a recurring parent.
     * Used by Tick-C to execute actual sends.
     */
    @Query("SELECT a FROM CampaignActivity a " +
           "WHERE a.status IN ('ACTIVE', 'READY') " +
           "AND a.isDeleted = false " +
           "AND a.nextExecutionAt <= :now " +
           "AND (a.scheduleType = 'ONE_TIME' OR a.parentActivity IS NOT NULL)")
    List<CampaignActivity> findDueExecutableActivities(@Param("now") LocalDateTime now);

    /**
     * Atomically lock an activity for execution by pushing its nextExecutionAt into the future.
     * Prevents multiple scheduler nodes from executing the same activity simultaneously.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE CampaignActivity a SET a.nextExecutionAt = :futureTime " +
           "WHERE a.id = :id AND a.nextExecutionAt <= :now AND a.status IN ('ACTIVE', 'READY')")
    int lockActivityForExecution(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);

    /**
     * Returns all child activities spawned by the given parent activity.
     * Used by the list API (?parentId=X).
     */
    @Query("SELECT a FROM CampaignActivity a " +
           "WHERE a.parentActivity.id = :parentId " +
           "AND a.isDeleted = false " +
           "ORDER BY a.createdAt DESC")
    List<CampaignActivity> findChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT a FROM CampaignActivity a " +
           "LEFT JOIN FETCH a.campaign " +
           "LEFT JOIN FETCH a.template " +
           "LEFT JOIN FETCH a.voiceTemplate " +
           "LEFT JOIN FETCH a.segment " +
           "WHERE a.id = :id")
    java.util.Optional<CampaignActivity> findByIdWithDetails(@Param("id") Long id);
}
