package com.smartstocks.product.repository;

import com.smartstocks.product.models.CampaignActivityExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignActivityExecutionLogRepository extends JpaRepository<CampaignActivityExecutionLog, Long> {

    List<CampaignActivityExecutionLog> findAllByActivityIdOrderByCreatedAtDesc(Long activityId);

    long countByActivityId(Long activityId);
}
