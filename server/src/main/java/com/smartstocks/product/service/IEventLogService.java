package com.smartstocks.product.service;

import com.smartstocks.product.dto.EventLogRequestDto;
import com.smartstocks.product.dto.EventLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.Map;

public interface IEventLogService {

    EventLogResponseDto logEvent(
            EventLogRequestDto request,
            String ipAddress,
            String userAgent,
            Map<String, String> requestHeaders,
            Principal principal);

    Page<EventLogResponseDto> getEvents(Long userId, String eventType, Pageable pageable, Principal principal);
}
