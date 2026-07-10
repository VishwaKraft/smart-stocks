package com.smartstocks.product.repository;

import com.smartstocks.product.models.WhatsappMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsappMessageLogRepository extends JpaRepository<WhatsappMessageLog, Long> {
    
    Optional<WhatsappMessageLog> findByWamid(String wamid);

    long countByCampaignIdAndStatus(Long campaignId, String status);
}
