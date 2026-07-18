package com.smartstocks.product.repository;

import com.smartstocks.product.models.VoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceTemplateRepository extends JpaRepository<VoiceTemplate, Long> {

    /** Find all active voice templates for a specific campaign. */
    List<VoiceTemplate> findByCampaignIdAndIsActiveTrue(Long campaignId);

    /** Find all active voice templates (global, not scoped to a campaign). */
    List<VoiceTemplate> findByIsActiveTrue();
}
