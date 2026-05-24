package com.smartstocks.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventLogRequestDto {

    @NotBlank(message = "event_type cannot be empty")
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
}
