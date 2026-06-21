package com.smartstocks.product.service;

import com.smartstocks.product.models.EventLog;
import com.smartstocks.product.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Lightweight internal event logger for Campaign Manager resources.
 * Persists structured events to event_logs asynchronously so it never
 * blocks the main request path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignEventLogger {

    private final EventLogRepository eventLogRepository;

    @Async
    public void log(String eventType, Map<String, Object> info) {
        try {
            EventLog e = new EventLog();
            e.setEventType(eventType);
            e.setEventInfo(info);
            e.setTimestamp(LocalDateTime.now());
            eventLogRepository.save(e);
        } catch (Exception ex) {
            log.error("CampaignEventLogger failed to persist event [{}]: {}", eventType, ex.getMessage());
        }
    }
}
