package com.smartstocks.product.repository;

import com.smartstocks.product.models.EmailBounceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailBounceEventRepository extends JpaRepository<EmailBounceEvent, Long> {

    List<EmailBounceEvent> findByActivityId(Long activityId);

    List<EmailBounceEvent> findByCampaignId(Long campaignId);

    List<EmailBounceEvent> findByEmailId(String emailId);

    long countByActivityId(Long activityId);
}
