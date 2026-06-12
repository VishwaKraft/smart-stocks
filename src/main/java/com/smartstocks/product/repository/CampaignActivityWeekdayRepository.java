package com.smartstocks.product.repository;

import com.smartstocks.product.models.CampaignActivityWeekday;
import com.smartstocks.product.models.Weekday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignActivityWeekdayRepository extends JpaRepository<CampaignActivityWeekday, Long> {

    List<CampaignActivityWeekday> findAllByActivityId(Long activityId);

    void deleteAllByActivityId(Long activityId);

    boolean existsByActivityIdAndWeekday(Long activityId, Weekday weekday);
}
