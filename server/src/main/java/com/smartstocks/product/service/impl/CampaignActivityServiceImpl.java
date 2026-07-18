package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CampaignActivityDto;
import com.smartstocks.product.dto.CreateActivityRequestDto;
import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.repository.CampaignActivityWeekdayRepository;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.SegmentRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.repository.TemplateRepository;
import com.smartstocks.product.repository.VoiceTemplateRepository;
import com.smartstocks.product.repository.CampaignSegmentUserRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ICampaignActivityService;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.provider.InfobipPeopleProvider;
import com.smartstocks.product.service.renderer.ITemplateRenderer;
import com.smartstocks.product.service.renderer.TemplateRendererFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CampaignActivityServiceImpl implements ICampaignActivityService {

    private final CampaignActivityRepository activityRepository;
    private final CampaignActivityWeekdayRepository weekdayRepository;
    private final CampaignRepository campaignRepository;
    private final TemplateRepository templateRepository;
    private final VoiceTemplateRepository voiceTemplateRepository;
    private final SegmentRepository segmentRepository;
    private final SegmentUserRepository segmentUserRepository;
    private final CampaignSegmentUserRepository campaignSegmentUserRepository;
    private final CampaignEventLogger eventLogger;
    private final ICampaignService campaignService;
    private final TemplateRendererFactory templateRendererFactory;

    @org.springframework.beans.factory.annotation.Value("${meta.oauth.client-secret:}")
    private String appSecret;

    @org.springframework.beans.factory.annotation.Value("${infobip.api-key:}")
    private String infobipApiKey;

    @org.springframework.beans.factory.annotation.Value("${infobip.people-base-url:}")
    private String infobipPeopleBaseUrl;

    @Override
    @Transactional
    public CampaignActivityDto createActivity(CreateActivityRequestDto request) {
        if (request.getActivityName() != null && !request.getActivityName().isEmpty()) {
            if (activityRepository.existsByActivityName(request.getActivityName())) {
                throw new IllegalArgumentException("Activity name already exists.");
            }
        }

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + request.getCampaignId()));

        Template template = null;
        VoiceTemplate voiceTemplate = null;

        if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.EMAIL) {
            if (request.getTemplateId() == null) {
                throw new IllegalArgumentException("Template ID is required for EMAIL campaigns.");
            }
            template = templateRepository.findById(request.getTemplateId())
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + request.getTemplateId()));
        } else if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.WHATSAPP) {
            if (request.getWhatsappTemplateName() == null || request.getWhatsappTemplateName().trim().isEmpty()) {
                throw new IllegalArgumentException("WhatsApp template name is required for WHATSAPP campaigns.");
            }
        } else if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.VOICE) {
            if (request.getVoiceTemplateId() == null) {
                throw new IllegalArgumentException("Voice template ID is required for VOICE campaigns.");
            }
            voiceTemplate = voiceTemplateRepository.findById(request.getVoiceTemplateId())
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Active voice template not found: " + request.getVoiceTemplateId()));
        }

        // Segment is mandatory
        if (request.getSegmentId() == null) {
            throw new IllegalArgumentException("Segment is required.");
        }
        Segment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + request.getSegmentId()));

        CampaignActivity activity = new CampaignActivity();
        activity.setCampaign(campaign);
        activity.setTemplate(template);
        activity.setVoiceTemplate(voiceTemplate);

        if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.WHATSAPP) {
            activity.setWhatsappTemplateName(request.getWhatsappTemplateName().trim());
            activity.setWhatsappLanguage(request.getWhatsappLanguage() != null ? request.getWhatsappLanguage() : "en_US");
        }
        
        activity.setSegment(segment);
        activity.setActivityName(request.getActivityName());
        activity.setScheduleType(request.getScheduleType());
        activity.setRecurrenceType(request.getRecurrenceType());
        activity.setExecutionDatetime(request.getExecutionDatetime());
        activity.setExecutionTime(request.getExecutionTime());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDayOfMonth(request.getDayOfMonth());
        activity.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        
        // Status always starts as GENERATING; user must trigger generation which moves it to NEW
        activity.setStatus(ActivityStatus.GENERATING);
        activity.setDeleted(false);
        activity.setNextExecutionAt(computeNextExecution(activity, LocalDateTime.now()));

        CampaignActivity saved = activityRepository.save(activity);

        // Persist weekday selections for WEEKLY recurrence
        if (request.getWeekdays() != null && !request.getWeekdays().isEmpty()) {
            persistWeekdays(saved, request.getWeekdays());
        }

        // Emit audit event
        Map<String, Object> info = new HashMap<>();
        info.put("activityId", saved.getId());
        info.put("activityName", saved.getActivityName());
        info.put("campaignId", saved.getCampaign().getId());
        info.put("segmentId", saved.getSegment() != null ? saved.getSegment().getId() : null);
        info.put("scheduleType", saved.getScheduleType());
        eventLogger.log("ACTIVITY_CREATED", info);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignActivityDto> getAllActivities() {
        return activityRepository.findAllByIsDeletedFalse().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignActivityDto> getActivitiesByCampaign(Long campaignId) {
        return activityRepository.findAllByCampaignIdAndIsDeletedFalse(campaignId).stream()
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

        if (request.getActivityName() != null && !request.getActivityName().isEmpty()) {
            if (activityRepository.existsByActivityNameAndIdNot(request.getActivityName(), id)) {
                throw new IllegalArgumentException("Activity name already exists.");
            }
        }

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + request.getCampaignId()));

        Template template = null;
        VoiceTemplate voiceTemplate = null;

        if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.EMAIL) {
            if (request.getTemplateId() == null) {
                throw new IllegalArgumentException("Template ID is required for EMAIL campaigns.");
            }
            template = templateRepository.findById(request.getTemplateId())
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + request.getTemplateId()));
        } else if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.WHATSAPP) {
            if (request.getWhatsappTemplateName() == null || request.getWhatsappTemplateName().trim().isEmpty()) {
                throw new IllegalArgumentException("WhatsApp template name is required for WHATSAPP campaigns.");
            }
        } else if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.VOICE) {
            if (request.getVoiceTemplateId() == null) {
                throw new IllegalArgumentException("Voice template ID is required for VOICE campaigns.");
            }
            voiceTemplate = voiceTemplateRepository.findById(request.getVoiceTemplateId())
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Active voice template not found: " + request.getVoiceTemplateId()));
        }

        Segment segment = null;
        if (request.getSegmentId() != null) {
            segment = segmentRepository.findById(request.getSegmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + request.getSegmentId()));
        }

        activity.setCampaign(campaign);
        activity.setTemplate(template);
        activity.setVoiceTemplate(voiceTemplate);

        if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.WHATSAPP) {
            activity.setWhatsappTemplateName(request.getWhatsappTemplateName().trim());
            activity.setWhatsappLanguage(request.getWhatsappLanguage() != null ? request.getWhatsappLanguage() : "en_US");
        } else {
            activity.setWhatsappTemplateName(null);
            activity.setWhatsappLanguage("en_US");
        }
        activity.setSegment(segment);
        activity.setActivityName(request.getActivityName());
        activity.setScheduleType(request.getScheduleType());
        activity.setRecurrenceType(request.getRecurrenceType());
        activity.setExecutionDatetime(request.getExecutionDatetime());
        activity.setExecutionTime(request.getExecutionTime());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDayOfMonth(request.getDayOfMonth());
        activity.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        activity.setNextExecutionAt(computeNextExecution(activity, LocalDateTime.now()));

        // Replace weekday rows
        weekdayRepository.deleteAllByActivityId(id);
        if (request.getWeekdays() != null && !request.getWeekdays().isEmpty()) {
            persistWeekdays(activity, request.getWeekdays());
        }

        CampaignActivity updatedActivity = activityRepository.save(activity);

        // Emit audit event
        Map<String, Object> info = new HashMap<>();
        info.put("activityId", updatedActivity.getId());
        info.put("activityName", updatedActivity.getActivityName());
        info.put("segmentId", updatedActivity.getSegment() != null ? updatedActivity.getSegment().getId() : null);
        eventLogger.log("ACTIVITY_UPDATED", info);

        return toDto(updatedActivity);
    }

    @Override
    @Transactional
    public boolean deleteActivity(Long id) {
        return activityRepository.findById(id).map(activity -> {
            activity.setDeleted(true);
            activityRepository.save(activity);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public CampaignActivityDto activateActivity(Long id) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));
        if (activity.isDeleted()) {
            throw new IllegalStateException("Cannot activate a soft-deleted activity.");
        }
        activity.setStatus(ActivityStatus.ACTIVE);
        CampaignActivity saved = activityRepository.save(activity);
        Map<String, Object> info = new HashMap<>();
        info.put("activityId", id);
        info.put("action", "ACTIVATED");
        eventLogger.log("ACTIVITY_STATUS_CHANGED", info);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CampaignActivityDto pauseActivity(Long id) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));
        if (activity.isDeleted()) {
            throw new IllegalStateException("Cannot pause a soft-deleted activity.");
        }
        activity.setStatus(ActivityStatus.PAUSED);
        CampaignActivity saved = activityRepository.save(activity);
        Map<String, Object> info = new HashMap<>();
        info.put("activityId", id);
        info.put("action", "PAUSED");
        eventLogger.log("ACTIVITY_STATUS_CHANGED", info);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void testTrigger(Long id, List<String> emailIds) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        Campaign campaign = activity.getCampaign();
        if (campaign.getEmailProviderType() == EmailProviderType.GMAIL) {
            String accessToken = campaign.getGoogleAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalStateException("Gmail is not authorized for this campaign.");
            }

            com.smartstocks.product.service.provider.GmailProvider gmailProvider = new com.smartstocks.product.service.provider.GmailProvider(accessToken);
            
            Template templateObj = activity.getTemplate();
            ITemplateRenderer renderer = templateRendererFactory.get(templateObj.getRendererType());
            
            // Inject tracking pixel for open tracking.
            // Use a unique nonce per test-fire so Google's image proxy cannot cache
            // a stale response. The test email address is used as the emailId.
            String testEmailId = (emailIds != null && !emailIds.isEmpty()) ? emailIds.get(0) : "test@example.com";
            String nonce = UUID.randomUUID().toString();
            String htmlWithPixel = campaignService.injectTrackingPixel(
                    templateObj.getHtmlBody(),
                    campaign.getCampaignCode(),
                    testEmailId,
                    activity.getId(),
                    nonce);
            
            // Render actual template with empty variables for test
            com.smartstocks.product.service.renderer.RenderedTemplate rendered = renderer.render(
                    templateObj.getSubject(),
                    htmlWithPixel,
                    new HashMap<>()
            );
            com.smartstocks.product.service.provider.SendResult result = gmailProvider.send(
                    rendered,
                    emailIds != null && !emailIds.isEmpty() ? emailIds : java.util.Collections.singletonList("test@example.com")
            );

            if (result.isAuthError()) {
                // Refresh token and retry
                accessToken = campaignService.refreshGoogleAccessToken(campaign.getId());
                gmailProvider = new com.smartstocks.product.service.provider.GmailProvider(accessToken);
                result = gmailProvider.send(
                        rendered,
                        emailIds != null && !emailIds.isEmpty() ? emailIds : java.util.Collections.singletonList("test@example.com")
                );
            }

            if (result.isSuccess()) {
                activity.setStatus(ActivityStatus.READY);
                activityRepository.save(activity);

                // Emit test fire event
                Map<String, Object> info = new HashMap<>();
                info.put("activityId", id);
                info.put("activityName", activity.getActivityName());
                info.put("recipients", emailIds);
                info.put("gmailResponse", result.getProviderResponse());
                eventLogger.log("ACTIVITY_TEST_FIRED", info);
            } else {
                throw new RuntimeException("Test email failed: " + result.getErrorMessage());
            }
        } else if (campaign.getCampaignType() == com.smartstocks.product.models.CampaignType.WHATSAPP) {
            String accessToken = campaign.getMetaAccessToken();
            String phoneNumberId = campaign.getMetaPhoneNumberId();
            if (accessToken == null || accessToken.isEmpty() || phoneNumberId == null || phoneNumberId.isEmpty()) {
                throw new IllegalStateException("WhatsApp is not authorized or configured for this campaign.");
            }

            com.smartstocks.product.service.provider.WhatsappProvider whatsappProvider = 
                    new com.smartstocks.product.service.provider.WhatsappProvider(accessToken, phoneNumberId, appSecret);

            String testPhone = (emailIds != null && !emailIds.isEmpty()) ? emailIds.get(0) : "";
            if (testPhone.isEmpty()) {
                throw new IllegalArgumentException("A test phone number must be provided in the emailIds list.");
            }

            String waTemplateName = activity.getWhatsappTemplateName();
            if (waTemplateName == null || waTemplateName.isBlank()) {
                throw new IllegalArgumentException("A WhatsApp template name must be configured in the activity before testing.");
            }

            com.smartstocks.product.service.provider.SendResult result = whatsappProvider.send(
                    testPhone,
                    waTemplateName,
                    "en_US"
            );

            if (result.isSuccess()) {
                activity.setStatus(ActivityStatus.READY);
                activityRepository.save(activity);

                // Emit test fire event
                Map<String, Object> info = new HashMap<>();
                info.put("activityId", id);
                info.put("activityName", activity.getActivityName());
                info.put("recipients", java.util.Collections.singletonList(testPhone));
                info.put("whatsappResponse", result.getProviderResponse());
                eventLogger.log("ACTIVITY_TEST_FIRED", info);
            } else {
                throw new RuntimeException("Test WhatsApp message failed: " + result.getErrorMessage());
            }
        } else {
            throw new UnsupportedOperationException("Test trigger currently only supports GMAIL and WHATSAPP.");
        }
    }

    @Override
    @Transactional
    public CampaignActivityDto cloneActivity(Long id, String newName) {
        if (newName != null && !newName.isEmpty() && activityRepository.existsByActivityName(newName)) {
            throw new IllegalArgumentException("Activity name already exists.");
        }

        CampaignActivity original = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        CampaignActivity clone = new CampaignActivity();
        clone.setCampaign(original.getCampaign());
        clone.setTemplate(original.getTemplate());
        clone.setWhatsappTemplateName(original.getWhatsappTemplateName());
        clone.setSegment(original.getSegment());
        clone.setActivityName(newName);
        clone.setScheduleType(original.getScheduleType());
        clone.setRecurrenceType(original.getRecurrenceType());
        clone.setExecutionDatetime(original.getExecutionDatetime());
        clone.setExecutionTime(original.getExecutionTime());
        clone.setStartDate(original.getStartDate());
        clone.setEndDate(original.getEndDate());
        clone.setDayOfMonth(original.getDayOfMonth());
        clone.setTimezone(original.getTimezone());
        clone.setStatus(ActivityStatus.NEW);
        clone.setNextExecutionAt(computeNextExecution(clone, LocalDateTime.now()));

        CampaignActivity saved = activityRepository.save(clone);

        List<Weekday> weekdays = weekdayRepository.findAllByActivityId(id).stream()
                .map(CampaignActivityWeekday::getWeekday)
                .collect(Collectors.toList());
        if (!weekdays.isEmpty()) {
            persistWeekdays(saved, weekdays);
        }

        // Emit audit event
        Map<String, Object> info = new HashMap<>();
        info.put("originalActivityId", id);
        info.put("newActivityId", saved.getId());
        info.put("newActivityName", saved.getActivityName());
        eventLogger.log("ACTIVITY_CLONED", info);

        return toDto(saved);
    }

    /**
     * Extracts users from the segment and puts them into CampaignSegmentUser.
     * Optionally registers people in Infobip for VOICE campaigns.
     * Changes status from GENERATING to NEW.
     */
    @Transactional
    public CampaignActivityDto generateActivityData(Long id) {
        CampaignActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        if (activity.getStatus() != ActivityStatus.GENERATING) {
            throw new IllegalStateException("Activity is not in GENERATING state.");
        }

        Segment segment = activity.getSegment();
        if (segment == null) {
            throw new IllegalStateException("No segment attached to this activity.");
        }

        List<com.smartstocks.product.models.SegmentUser> segmentUsers = segmentUserRepository.findBySegmentId(segment.getId());

        InfobipPeopleProvider peopleProvider = null;
        if (activity.getCampaign().getCampaignType() == com.smartstocks.product.models.CampaignType.VOICE) {
            if (infobipApiKey == null || infobipApiKey.isBlank()) {
                throw new IllegalStateException("Infobip API key is not configured.");
            }
            peopleProvider = new InfobipPeopleProvider(infobipApiKey, infobipPeopleBaseUrl);
        }

        campaignSegmentUserRepository.deleteAllByActivityId(id);

        int count = 0;
        for (com.smartstocks.product.models.SegmentUser su : segmentUsers) {
            CampaignSegmentUser csu = new CampaignSegmentUser();
            csu.setActivity(activity);
            csu.setEmailId(su.getEmailId());
            csu.setPhoneNumber(su.getPhoneNumber());
            csu.setUserId(su.getUserId());
            csu.setData(su.getData());

            if (peopleProvider != null && csu.getPhoneNumber() != null && !csu.getPhoneNumber().isBlank()) {
                String personId = peopleProvider.createPerson(csu);
                if (personId != null) {
                    csu.setInfobipPersonCreated(true);
                    if (!personId.equals("existing")) {
                        csu.setInfobipPersonId(personId);
                    }
                }
            }

            campaignSegmentUserRepository.save(csu);
            count++;
        }

        activity.setRecipientCount(count);
        activity.setStatus(ActivityStatus.NEW);
        CampaignActivity saved = activityRepository.save(activity);

        Map<String, Object> info = new HashMap<>();
        info.put("activityId", saved.getId());
        info.put("activityName", saved.getActivityName());
        info.put("recipientCount", count);
        eventLogger.log("ACTIVITY_GENERATED", info);

        return toDto(saved);
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
                .templateId(a.getTemplate() != null ? a.getTemplate().getId() : null)
                .templateName(a.getTemplate() != null ? a.getTemplate().getName() : null)
                .whatsappTemplateName(a.getWhatsappTemplateName())
                .whatsappLanguage(a.getWhatsappLanguage())
                .voiceTemplateId(a.getVoiceTemplate() != null ? a.getVoiceTemplate().getId() : null)
                .voiceTemplateName(a.getVoiceTemplate() != null ? a.getVoiceTemplate().getName() : null)
                .segmentId(a.getSegment() != null ? a.getSegment().getId() : null)
                .segmentName(a.getSegment() != null ? a.getSegment().getName() : null)
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
                .isDeleted(a.isDeleted())
                .weekdays(weekdays)
                .recipientCount(a.getRecipientCount())
                .nextExecutionAt(a.getNextExecutionAt())
                .lastExecutionAt(a.getLastExecutionAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
