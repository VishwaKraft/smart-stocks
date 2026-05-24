package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.EventLogRequestDto;
import com.smartstocks.product.dto.EventLogResponseDto;
import com.smartstocks.product.models.EventLog;
import com.smartstocks.product.models.User;
import com.smartstocks.product.repository.EventLogRepository;
import com.smartstocks.product.service.IEventLogService;
import com.smartstocks.product.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
public class EventLogServiceImpl implements IEventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private IUserService userService;

    @Override
    public EventLogResponseDto logEvent(EventLogRequestDto request, String ipAddress, String userAgent, Principal principal) {
        EventLog eventLog = new EventLog();
        eventLog.setEventType(request.getEventType());
        eventLog.setEventInfo(request.getEventInfo());
        eventLog.setUserId(resolveUserId(request.getUserId(), principal));
        eventLog.setIpAddress(firstNonBlank(request.getIpAddress(), ipAddress));
        eventLog.setUserAgent(firstNonBlank(request.getUserAgent(), userAgent, ""));
        eventLog.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());

        EventLog saved = eventLogRepository.save(eventLog);
        return toResponseDto(saved);
    }

    @Override
    public Page<EventLogResponseDto> getEvents(Long userId, String eventType, Pageable pageable, Principal principal) {
        Long effectiveUserId = resolveQueryUserId(userId, principal);
        Page<EventLog> page;

        if (effectiveUserId != null && eventType != null) {
            page = eventLogRepository.findByUserIdAndEventType(effectiveUserId, eventType, pageable);
        } else if (effectiveUserId != null) {
            page = eventLogRepository.findByUserId(effectiveUserId, pageable);
        } else if (eventType != null) {
            page = eventLogRepository.findByEventType(eventType, pageable);
        } else {
            page = eventLogRepository.findAll(pageable);
        }

        return page.map(this::toResponseDto);
    }

    private Long resolveUserId(Long requestedUserId, Principal principal) {
        if (requestedUserId != null) {
            return requestedUserId;
        }
        if (principal != null) {
            User user = userService.getUserByEmail(principal.getName());
            return user.getUserId();
        }
        return null;
    }

    private Long resolveQueryUserId(Long requestedUserId, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User currentUser = userService.getUserByEmail(principal.getName());
        if (requestedUserId == null) {
            return currentUser.getUserId();
        }
        if (!requestedUserId.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot query events for another user");
        }
        return requestedUserId;
    }

    private EventLogResponseDto toResponseDto(EventLog eventLog) {
        return new EventLogResponseDto(
                eventLog.getEventId(),
                eventLog.getEventType(),
                eventLog.getUserId(),
                eventLog.getEventInfo(),
                eventLog.getIpAddress(),
                eventLog.getUserAgent(),
                eventLog.getTimestamp(),
                eventLog.getCreatedAt()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
