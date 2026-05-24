package com.smartstocks.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventLogResponseDto {

    @JsonProperty("event_id")
    private Long eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("event_info")
    private Map<String, Object> eventInfo;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    private LocalDateTime timestamp;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
