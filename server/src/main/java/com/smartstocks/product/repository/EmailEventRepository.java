package com.smartstocks.product.repository;

import com.smartstocks.product.models.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {

    Optional<EmailEvent> findByEventName(String eventName);

    List<EmailEvent> findAllByIsActiveTrueOrderByCreatedAtDesc();

    List<EmailEvent> findAllByOrderByCreatedAtDesc();

    boolean existsByEventName(String eventName);
}
