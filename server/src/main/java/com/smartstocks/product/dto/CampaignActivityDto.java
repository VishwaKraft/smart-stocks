package com.smartstocks.product.dto;

import com.smartstocks.product.models.ActivityStatus;
import com.smartstocks.product.models.RecurrenceType;
import com.smartstocks.product.models.ScheduleType;
import com.smartstocks.product.models.Weekday;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignActivityDto {

    private Long id;
    private Long campaignId;
    private String campaignName;
    private Long templateId;
    private String templateName;
    private String whatsappTemplateName;
    private Long segmentId;
    private String segmentName;
    private String activityName;
    private ScheduleType scheduleType;
    private RecurrenceType recurrenceType;
    private LocalDateTime executionDatetime;
    private LocalTime executionTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayOfMonth;
    private String timezone;
    private ActivityStatus status;
    private boolean isDeleted;
    private List<Weekday> weekdays;
    private LocalDateTime nextExecutionAt;
    private LocalDateTime lastExecutionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
