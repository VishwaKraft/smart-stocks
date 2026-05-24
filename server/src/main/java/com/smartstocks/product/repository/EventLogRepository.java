package com.smartstocks.product.repository;

import com.smartstocks.product.models.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    Page<EventLog> findByUserId(Long userId, Pageable pageable);

    Page<EventLog> findByEventType(String eventType, Pageable pageable);

    Page<EventLog> findByUserIdAndEventType(Long userId, String eventType, Pageable pageable);
}
