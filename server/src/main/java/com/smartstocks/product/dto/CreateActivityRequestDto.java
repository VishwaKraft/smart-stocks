package com.smartstocks.product.dto;

import com.smartstocks.product.models.ActivityStatus;
import com.smartstocks.product.models.RecurrenceType;
import com.smartstocks.product.models.ScheduleType;
import com.smartstocks.product.models.Weekday;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequestDto {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    /**
     * Required for EMAIL campaigns. Leave null for WHATSAPP campaigns
     * and set whatsappTemplateName instead.
     */
    private Long templateId;

    /** Required for WHATSAPP campaigns – the approved Meta template name (e.g. "hello_world"). */
    private String whatsappTemplateName;

    @NotNull(message = "Segment ID is required")
    private Long segmentId;

    private String activityName;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    /** Required when scheduleType = ONE_TIME */
    private LocalDateTime executionDatetime;

    /** Required when scheduleType = RECURRING */
    private RecurrenceType recurrenceType;

    /** Time of day for recurring schedules */
    private LocalTime executionTime;

    /** For WEEKLY recurrence */
    private List<Weekday> weekdays;

    /** For MONTHLY recurrence (1-31) */
    private Integer dayOfMonth;

    private LocalDate startDate;
    private LocalDate endDate;

    private String timezone;

    // NOTE: status is NOT accepted at creation time.
    // On creation the status is always set to NEW.
    // Use POST /api/activities/{id}/activate or /pause to change state.
}

