package com.smartstocks.product.repository;

import com.smartstocks.product.models.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByCampaignCode(String campaignCode);

    boolean existsByCampaignCode(String campaignCode);
}
