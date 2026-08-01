package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerEventEmailResponseDto {

    private String eventName;
    private String displayName;
    private boolean success;
    private int recipientCount;
    private List<String> recipients;
    private String providerResponse;
    private String errorMessage;

    /** DB ID of the audit log entry created for this trigger call. */
    private Long triggerLogId;

    private LocalDateTime triggeredAt;
}
