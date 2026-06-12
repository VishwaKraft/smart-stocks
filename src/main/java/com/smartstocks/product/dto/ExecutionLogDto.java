package com.smartstocks.product.dto;

import com.smartstocks.product.models.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLogDto {

    private Long id;
    private Long activityId;
    private Long campaignId;
    private String campaignName;
    private Long templateId;
    private String templateName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private ExecutionStatus status;
    private Integer recipientCount;
    private String providerResponse;
    private String errorMessage;
    private LocalDateTime createdAt;
}
