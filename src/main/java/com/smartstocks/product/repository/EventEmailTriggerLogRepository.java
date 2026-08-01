package com.smartstocks.product.repository;

import com.smartstocks.product.models.EventEmailTriggerLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventEmailTriggerLogRepository extends JpaRepository<EventEmailTriggerLog, Long> {

    List<EventEmailTriggerLog> findByEventNameOrderByCreatedAtDesc(String eventName);

    Page<EventEmailTriggerLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByEmailEventId(Long emailEventId);

    long countByEmailEventIdAndSuccessTrue(Long emailEventId);
}
