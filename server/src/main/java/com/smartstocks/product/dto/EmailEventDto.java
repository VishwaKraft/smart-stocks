package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEventDto {

    private Long id;
    private String eventName;
    private String displayName;
    private String description;

    // Campaign info
    private Long campaignId;
    private String campaignName;
    private String campaignCode;
    private String emailProviderType;

    // Template info
    private Long templateId;
    private String templateName;
    private String templateSubject;

    private Boolean isActive;

    /** Total number of times this event has been triggered. */
    private long triggerCount;

    /** Number of successful trigger calls. */
    private long successCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
