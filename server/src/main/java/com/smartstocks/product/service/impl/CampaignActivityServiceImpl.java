package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CampaignActivityDto;
import com.smartstocks.product.dto.CreateActivityRequestDto;
import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.repository.CampaignActivityWeekdayRepository;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.TemplateRepository;
import com.smartstocks.product.service.ICampaignActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CampaignActivityServiceImpl implements ICampaignActivityService {

    private final CampaignActivityRepository activityRepository;
    private final CampaignActivityWeekdayRepository weekdayRepository;
    private final CampaignRepository campaignRepository;
    private final TemplateRepository templateRepository;

    @Override
    @Transactional
    public CampaignActivityDto createActivity(CreateActivityRequestDto request) {
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + request.getCampaignId()));

        Template template = templateRepository.findById(request.getTemplateId())
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + request.getTemplateId()));

        CampaignActivity activity = new CampaignActivity();
        activity.setCampaign(campaign);
        activity.setTemplate(template);
        activity.setActivityName(request.getActivityName());
        activity.setScheduleType(request.getScheduleType());
        activity.setRecurrenceType(request.getRecurrenceType());
        activity.setExecutionDatetime(request.getExecutionDatetime());
        activity.setExecutionTime(request.getExecutionTime());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDayOfMonth(request.getDayOfMonth());
        activity.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        activity.setStatus(request.getStatus() != null ? request.getStatus() : ActivityStatus.NEW);
        activity.setNextExecutionAt(computeNextExecution(activity, LocalDateTime.now()));

        CampaignActivity saved = activityRepository.save(activity);

        // Persist weekday selections for WEEKLY recurrence
        if (request.getWeekdays() != null && !request.getWeekdays().isEmpty()) {
            persistWeekdays(saved, request.getWeekdays());
        }

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignActivityDto> getAllActivities() {
        return activityRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignActivityDto> getActivitiesByCampaign(Long campaignId) {
        return activityRepository.findAllByCampaignId(campaignId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignActivityDto> getActivityById(Long id) {
        return activityRepository.findById(id).map(this::toDto);
    }

    @Override
    @Transactional
    public CampaignActivityDto updateActivity(Long id, CreateActivityRequestDto request) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + request.getCampaignId()));

        Template template = templateRepository.findById(request.getTemplateId())
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + request.getTemplateId()));

        activity.setCampaign(campaign);
        activity.setTemplate(template);
        activity.setActivityName(request.getActivityName());
        activity.setScheduleType(request.getScheduleType());
        activity.setRecurrenceType(request.getRecurrenceType());
        activity.setExecutionDatetime(request.getExecutionDatetime());
        activity.setExecutionTime(request.getExecutionTime());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDayOfMonth(request.getDayOfMonth());
        activity.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        if (request.getStatus() != null) {
            activity.setStatus(request.getStatus());
        }
        activity.setNextExecutionAt(computeNextExecution(activity, LocalDateTime.now()));

        // Replace weekday rows
        weekdayRepository.deleteAllByActivityId(id);
        if (request.getWeekdays() != null && !request.getWeekdays().isEmpty()) {
            persistWeekdays(activity, request.getWeekdays());
        }

        return toDto(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public boolean deleteActivity(Long id) {
        return activityRepository.findById(id).map(activity -> {
            activity.setStatus(ActivityStatus.CANCELLED);
            activityRepository.save(activity);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public void testTrigger(Long id) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        Campaign campaign = activity.getCampaign();
        if (campaign.getEmailProviderType() == EmailProviderType.GMAIL) {
            String accessToken = campaign.getGoogleAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalStateException("Gmail is not authorized for this campaign.");
            }

            com.smartstocks.product.service.provider.GmailProvider gmailProvider = new com.smartstocks.product.service.provider.GmailProvider(accessToken);
            
            // Dummy test payload
            com.smartstocks.product.service.renderer.RenderedTemplate rendered = new com.smartstocks.product.service.renderer.RenderedTemplate(
                    "Test Trigger: " + activity.getActivityName(),
                    "This is a test email triggered from Smart Stocks campaign manager."
            );
            com.smartstocks.product.service.provider.SendResult result = gmailProvider.send(
                    rendered,
                    java.util.Collections.singletonList("test@example.com")
            );

            if (result.isSuccess()) {
                activity.setStatus(ActivityStatus.READY);
                activityRepository.save(activity);
            } else {
                throw new RuntimeException("Test email failed: " + result.getErrorMessage());
            }
        } else {
            throw new UnsupportedOperationException("Test trigger currently only supports GMAIL.");
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void persistWeekdays(CampaignActivity activity, List<Weekday> weekdays) {
        weekdays.forEach(day -> {
            CampaignActivityWeekday wd = new CampaignActivityWeekday();
            wd.setActivity(activity);
            wd.setWeekday(day);
            weekdayRepository.save(wd);
        });
    }

    /**
     * Compute the next execution instant from the current time given the activity schedule.
     */
    public LocalDateTime computeNextExecution(CampaignActivity activity, LocalDateTime now) {
        ZoneId zone = resolveZone(activity.getTimezone());

        if (activity.getScheduleType() == ScheduleType.ONE_TIME) {
            return activity.getExecutionDatetime();
        }

        // RECURRING
        LocalDate today = ZonedDateTime.of(now, ZoneId.systemDefault()).withZoneSameInstant(zone).toLocalDate();
        RecurrenceType rType = activity.getRecurrenceType();

        if (rType == RecurrenceType.DAILY) {
            LocalDateTime candidate = today.atTime(activity.getExecutionTime());
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }

        if (rType == RecurrenceType.MONTHLY) {
            int day = activity.getDayOfMonth() != null ? activity.getDayOfMonth() : 1;
            LocalDateTime candidate = today.withDayOfMonth(Math.min(day, today.lengthOfMonth()))
                    .atTime(activity.getExecutionTime());
            if (!candidate.isAfter(now)) {
                LocalDate nextMonth = today.plusMonths(1);
                candidate = nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()))
                        .atTime(activity.getExecutionTime());
            }
            return candidate;
        }

        // WEEKLY — find the nearest configured weekday
        if (rType == RecurrenceType.WEEKLY) {
            // fallback: +7 days if no weekdays configured
            return now.plusDays(7);
        }

        return now.plusDays(1);
    }

    private ZoneId resolveZone(String timezone) {
        try {
            return ZoneId.of(timezone != null ? timezone : "UTC");
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    private CampaignActivityDto toDto(CampaignActivity a) {
        List<Weekday> weekdays = weekdayRepository.findAllByActivityId(a.getId()).stream()
                .map(CampaignActivityWeekday::getWeekday)
                .collect(Collectors.toList());

        return CampaignActivityDto.builder()
                .id(a.getId())
                .campaignId(a.getCampaign().getId())
                .campaignName(a.getCampaign().getName())
                .templateId(a.getTemplate().getId())
                .templateName(a.getTemplate().getName())
                .activityName(a.getActivityName())
                .scheduleType(a.getScheduleType())
                .recurrenceType(a.getRecurrenceType())
                .executionDatetime(a.getExecutionDatetime())
                .executionTime(a.getExecutionTime())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .dayOfMonth(a.getDayOfMonth())
                .timezone(a.getTimezone())
                .status(a.getStatus())
                .weekdays(weekdays)
                .nextExecutionAt(a.getNextExecutionAt())
                .lastExecutionAt(a.getLastExecutionAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
