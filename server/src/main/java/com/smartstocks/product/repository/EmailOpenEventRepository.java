package com.smartstocks.product.repository;

import com.smartstocks.product.models.EmailOpenEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailOpenEventRepository extends JpaRepository<EmailOpenEvent, Long> {

    long countByCampaignId(Long campaignId);

    long countByCampaign(String campaign);
}
