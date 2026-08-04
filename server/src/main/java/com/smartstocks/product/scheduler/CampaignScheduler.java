package com.smartstocks.product.scheduler;

import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.CampaignActivityExecutionLogRepository;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.repository.EmailBounceEventRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.repository.WhatsappMessageLogRepository;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.impl.CampaignActivityServiceImpl;
import com.smartstocks.product.service.provider.EmailProviderFactory;
import com.smartstocks.product.service.provider.GmailProvider;
import com.smartstocks.product.service.provider.IEmailProvider;
import com.smartstocks.product.service.provider.SendResult;
import com.smartstocks.product.service.provider.WhatsappProvider;
import com.smartstocks.product.service.provider.InfobipVoiceProvider;
import com.smartstocks.product.service.renderer.ITemplateRenderer;
import com.smartstocks.product.service.renderer.RenderedTemplate;
import com.smartstocks.product.service.renderer.TemplateRendererFactory;
import com.smartstocks.product.service.IShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs every 60 seconds.
 * For each due READY/ACTIVE activity (not soft-deleted):
 *   - Loads all SegmentUsers from the activity's segment
 *   - Renders the template individually per recipient using their data as variables
 *   - Injects a per-recipient tracking pixel (with emailId + activityId params)
 *   - Sends via the configured email provider (extensible via EmailProviderFactory)
 *   - Records bounces to the email_bounce_events table
 *   - Logs the aggregate execution result
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignActivityRepository activityRepository;
    private final CampaignActivityExecutionLogRepository logRepository;
    private final SegmentUserRepository segmentUserRepository;
    private final EmailBounceEventRepository bounceEventRepository;
    private final TemplateRendererFactory rendererFactory;
    private final EmailProviderFactory emailProviderFactory;
    private final CampaignActivityServiceImpl activityService;
    private final ICampaignService campaignService;
    private final WhatsappMessageLogRepository whatsappMessageLogRepository;
    private final IShortLinkService shortLinkService;

    private final com.smartstocks.product.repository.CampaignSegmentUserRepository campaignSegmentUserRepository;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Value("${app.short-link.base-url:http://localhost:8080}")
    private String shortLinkBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${meta.oauth.client-secret:}")
    private String appSecret;

    @org.springframework.beans.factory.annotation.Value("${infobip.api-key:}")
    private String infobipApiKey;

    @org.springframework.beans.factory.annotation.Value("${infobip.base-url:}")
    private String infobipBaseUrl;

    /**
     * TICK A — Runs every minute.
     * Finds RECURRING parent activities that are due and spawns a fresh ONE_TIME child
     * activity for each occurrence. The parent is never executed directly; it acts as
     * a permanent schedule placeholder.
     * Stops spawning for a parent when:
     *   - parent status is PAUSED  (user has paused the recurring activity)
     *   - parent endDate has been passed
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void spawnRecurringChildren() {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignActivity> dueParents = activityRepository.findDueRecurringParents(now);

        if (dueParents.isEmpty()) {
            return;
        }

        log.info("[Scheduler][TickA] Spawning children for {} recurring parent(s) at {}", dueParents.size(), now);

        for (CampaignActivity parent : dueParents) {
            try {
                spawnChild(parent, now);
            } catch (Exception ex) {
                log.error("[Scheduler][TickA] Failed to spawn child for parent [{}]: {}",
                        parent.getId(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * TICK B — Runs every minute.
     * Finds child activities in GENERATING status whose scheduled send time is
     * within the next 5 minutes and auto-generates their recipient data.
     * This ensures the segment snapshot is fresh just before the send.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void generateDueChildren() {
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(5);
        List<CampaignActivity> children = activityRepository.findChildrenDueForGeneration(threshold);

        if (children.isEmpty()) {
            return;
        }

        log.info("[Scheduler][TickB] Auto-generating data for {} child activit(y/ies)", children.size());

        for (CampaignActivity child : children) {
            try {
                activityService.generateActivityData(child.getId());
                // After GENERATING → NEW, auto-activate so the child is picked up by Tick-C
                child.setStatus(ActivityStatus.ACTIVE);
                // nextExecutionAt for the child is its executionDatetime (ONE_TIME)
                child.setNextExecutionAt(child.getExecutionDatetime());
                activityRepository.save(child);
                log.info("[Scheduler][TickB] Child [{}] generated and activated (scheduled for {})",
                        child.getId(), child.getExecutionDatetime());
            } catch (Exception ex) {
                log.error("[Scheduler][TickB] Failed to generate child [{}]: {}",
                        child.getId(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * TICK C — Runs every minute.
     * Executes due ONE_TIME activities (including auto-spawned children).
     * Recurring parents are never included here — they are handled by Tick-A only.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processExecutableActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignActivity> dueActivities = activityRepository.findDueExecutableActivities(now);

        if (dueActivities.isEmpty()) {
            return;
        }

        log.info("[Scheduler][TickC] Executing {} activit(y/ies) at {}", dueActivities.size(), now);

        for (CampaignActivity activity : dueActivities) {
            executeActivity(activity, now);
        }
    }

    // -----------------------------------------------------------------------
    // Child spawn helper (Tick-A)
    // -----------------------------------------------------------------------

    /**
     * Spawns a ONE_TIME child activity for one occurrence of the given recurring parent.
     * The child:
     *   - copies all campaign/template/segment config from the parent
     *   - uses the parent's current nextExecutionAt as its executionDatetime
     *   - is named "<parentName>_<epochMillis>" for uniqueness
     *   - starts in GENERATING status (Tick-B will auto-generate data before send)
     * After spawning, the parent's nextExecutionAt is advanced to the next occurrence.
     */
    private void spawnChild(CampaignActivity parent, LocalDateTime now) {
        // Guard: skip if parent is past its end date
        if (parent.getEndDate() != null && now.toLocalDate().isAfter(parent.getEndDate())) {
            log.info("[Scheduler][TickA] Parent [{}] past end date ({}), marking COMPLETED",
                    parent.getId(), parent.getEndDate());
            parent.setStatus(ActivityStatus.COMPLETED);
            parent.setNextExecutionAt(null);
            activityRepository.save(parent);
            return;
        }

        // Occurrence time = the parent's current nextExecutionAt
        LocalDateTime occurrenceAt = parent.getNextExecutionAt();
        long epochMillis = occurrenceAt
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        String childName = parent.getActivityName() + "_" + epochMillis;

        CampaignActivity child = new CampaignActivity();
        child.setCampaign(parent.getCampaign());
        child.setTemplate(parent.getTemplate());
        child.setVoiceTemplate(parent.getVoiceTemplate());
        child.setWhatsappTemplateName(parent.getWhatsappTemplateName());
        child.setWhatsappLanguage(parent.getWhatsappLanguage());
        child.setSegment(parent.getSegment());
        child.setActivityName(childName);
        child.setScheduleType(ScheduleType.ONE_TIME);
        child.setRecurrenceType(null);
        child.setExecutionDatetime(occurrenceAt);
        child.setTimezone(parent.getTimezone());
        child.setStatus(ActivityStatus.GENERATING);
        child.setDeleted(false);
        child.setParentActivity(parent);
        // nextExecutionAt = occurrenceAt so Tick-C can pick it up when it becomes ACTIVE
        child.setNextExecutionAt(occurrenceAt);

        activityRepository.save(child);
        log.info("[Scheduler][TickA] Spawned child [{}] ('{}') for parent [{}] scheduled at {}",
                child.getId(), childName, parent.getId(), occurrenceAt);

        // Advance the parent to the next occurrence
        LocalDateTime next = activityService.computeNextExecution(parent, occurrenceAt);
        parent.setNextExecutionAt(next);
        parent.setLastExecutionAt(now);
        activityRepository.save(parent);
        log.info("[Scheduler][TickA] Parent [{}] next occurrence at {}", parent.getId(), next);
    }

    // -----------------------------------------------------------------------
    // Execute helper (Tick-C)
    // -----------------------------------------------------------------------
    private void executeActivity(CampaignActivity activity, LocalDateTime now) {
        LocalDateTime startedAt = LocalDateTime.now();
        Campaign campaign = activity.getCampaign();
        Template template = activity.getTemplate();
        VoiceTemplate voiceTemplate = activity.getVoiceTemplate();
        String templateName = (template != null) ? template.getName() : 
                              (voiceTemplate != null) ? voiceTemplate.getName() : 
                              activity.getWhatsappTemplateName();

        log.info("[Scheduler] Executing activity [{}] for campaign [{}] using template [{}]",
                activity.getId(), campaign.getName(), templateName);

        // 1. Load all segment users for this activity's segment
        List<SegmentUser> segmentUsers = (activity.getSegment() != null)
                ? segmentUserRepository.findBySegmentId(activity.getSegment().getId())
                : Collections.emptyList();

        if (segmentUsers.isEmpty()) {
            log.warn("[Scheduler] Activity [{}] has no recipients in segment. Skipping.", activity.getId());
            return;
        }

        // Branch on campaign type
        if (campaign.getCampaignType() == CampaignType.VOICE) {
            executeVoiceActivity(activity, campaign, voiceTemplate, startedAt);
        } else if (campaign.getCampaignType() == CampaignType.WHATSAPP) {
            executeWhatsappActivity(activity, campaign, segmentUsers, startedAt);
        } else {
            executeEmailActivity(activity, campaign, template, segmentUsers, startedAt, now);
        }
    }

    // -----------------------------------------------------------------------
    // WhatsApp execution path
    // -----------------------------------------------------------------------
    private void executeWhatsappActivity(CampaignActivity activity, Campaign campaign,
                                          List<SegmentUser> segmentUsers, LocalDateTime startedAt) {
        if (campaign.getMetaAccessToken() == null || campaign.getMetaAccessToken().isBlank()) {
            log.error("[Scheduler] WhatsApp activity [{}] skipped – no Meta access token. Use 'Sign in with Meta'.", activity.getId());
            return;
        }
        if (campaign.getMetaPhoneNumberId() == null || campaign.getMetaPhoneNumberId().isBlank()) {
            log.error("[Scheduler] WhatsApp activity [{}] skipped – no Meta Phone Number ID configured.", activity.getId());
            return;
        }

        WhatsappProvider whatsappProvider = new WhatsappProvider(
                campaign.getMetaAccessToken(),
                campaign.getMetaPhoneNumberId(),
                appSecret);

        String waTemplateName = activity.getWhatsappTemplateName();
        if (waTemplateName == null || waTemplateName.isBlank()) {
            log.error("[Scheduler] WhatsApp activity [{}] skipped – missing WhatsApp template name.", activity.getId());
            return;
        }
        String waLanguage = activity.getWhatsappLanguage() != null ? activity.getWhatsappLanguage() : "en_US";

        int sentCount = 0;
        int bounceCount = 0;

        for (SegmentUser recipient : segmentUsers) {
            String phone = recipient.getPhoneNumber();
            if (phone == null || phone.isBlank()) {
                log.warn("[Scheduler] Recipient [{}] has no phone number – skipping for WhatsApp activity [{}]",
                        recipient.getEmailId(), activity.getId());
                bounceCount++;
                continue;
            }

            try {
                SendResult result = whatsappProvider.send(phone, waTemplateName, waLanguage);
                if (result.isSuccess()) {
                    sentCount++;
                    String wamid = result.getProviderResponse();
                    if (wamid != null && !wamid.isEmpty() && !wamid.equals("unknown")) {
                        WhatsappMessageLog logEntry = WhatsappMessageLog.builder()
                                .wamid(wamid)
                                .campaignId(campaign.getId())
                                .activityId(activity.getId())
                                .phoneNumber(phone)
                                .status("sent")
                                .sentAt(LocalDateTime.now())
                                .build();
                        whatsappMessageLogRepository.save(logEntry);
                    }
                    log.debug("[Scheduler] WhatsApp sent to [{}] for activity [{}] with wamid [{}]", phone, activity.getId(), wamid);
                } else {
                    bounceCount++;
                    log.warn("[Scheduler] WhatsApp failed for [{}], activity [{}]: {}",
                            phone, activity.getId(), result.getErrorMessage());
                    recordBounce(activity, campaign, phone, result);
                }
            } catch (Exception ex) {
                bounceCount++;
                log.error("[Scheduler] Exception sending WhatsApp to [{}], activity [{}]: {}",
                        phone, activity.getId(), ex.getMessage(), ex);
                recordBounce(activity, campaign, phone, SendResult.failure("Exception: " + ex.getMessage()));
            }
        }

        String providerResponse = String.format("whatsapp: sent=%d, failed=%d, total=%d",
                sentCount, bounceCount, segmentUsers.size());

        CampaignActivityExecutionLog executionLog = CampaignActivityExecutionLog.builder()
                .activity(activity)
                .campaign(campaign)
                .template(activity.getTemplate())
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .status(sentCount > 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .recipientCount(sentCount)
                .providerResponse(providerResponse)
                .errorMessage(bounceCount > 0 ? bounceCount + " recipient(s) failed." : null)
                .build();
        logRepository.save(executionLog);

        activity.setLastExecutionAt(LocalDateTime.now());
        updateActivityAfterExecution(activity, LocalDateTime.now());
        activityRepository.save(activity);
    }

    // -----------------------------------------------------------------------
    // Voice execution path
    // -----------------------------------------------------------------------
    private void executeVoiceActivity(CampaignActivity activity, Campaign campaign,
                                       VoiceTemplate voiceTemplate, LocalDateTime startedAt) {
        if (infobipApiKey == null || infobipApiKey.isBlank()) {
            log.error("[Scheduler] Voice activity [{}] skipped – missing infobip.api-key.", activity.getId());
            return;
        }
        if (campaign.getInfobipSenderNumber() == null || campaign.getInfobipSenderNumber().isBlank()) {
            log.error("[Scheduler] Voice activity [{}] skipped – no Infobip sender number configured.", activity.getId());
            return;
        }

        InfobipVoiceProvider voiceProvider = new InfobipVoiceProvider(infobipApiKey, infobipBaseUrl);
        String senderNumber = campaign.getInfobipSenderNumber();

        // 1. Load CampaignSegmentUser (already populated during GENERATE stage)
        List<CampaignSegmentUser> segmentUsers = campaignSegmentUserRepository.findByActivityId(activity.getId());
        if (segmentUsers.isEmpty()) {
            log.warn("[Scheduler] Voice activity [{}] has no generated recipients. Did you run /generate?", activity.getId());
            return;
        }

        // Fetch external data if dataSourceUrl is present
        Map<String, Object> externalData = new HashMap<>();
        if (voiceTemplate.getDataSourceUrl() != null && !voiceTemplate.getDataSourceUrl().isBlank()) {
            try {
                Map<String, Object> apiResponse = restTemplate.getForObject(voiceTemplate.getDataSourceUrl(), Map.class);
                if (apiResponse != null) {
                    externalData.putAll(apiResponse);
                }
            } catch (Exception e) {
                log.error("[Scheduler] Failed to fetch external data from URL: {}", voiceTemplate.getDataSourceUrl(), e);
            }
        }

        int sentCount = 0;
        int bounceCount = 0;

        for (CampaignSegmentUser recipient : segmentUsers) {
            String phone = recipient.getPhoneNumber();
            if (phone == null || phone.isBlank()) {
                log.warn("[Scheduler] Recipient [{}] has no phone number – skipping for voice activity [{}]",
                        recipient.getEmailId(), activity.getId());
                bounceCount++;
                continue;
            }

            try {
                // Build variable map including extra segment data
                Map<String, Object> variables = new HashMap<>();
                variables.put("firstName", recipient.getFirstName() != null ? recipient.getFirstName() : "");
                variables.put("lastName", recipient.getLastName() != null ? recipient.getLastName() : "");
                if (recipient.getData() != null) {
                    variables.putAll(recipient.getData());
                }
                variables.putAll(externalData); // Merge external data

                // Render the TTS message
                ITemplateRenderer renderer = rendererFactory.get(RendererType.HANDLEBARS);
                RenderedTemplate rendered = renderer.render(
                        "", // no subject
                        voiceTemplate.getMessageText(),
                        variables);
                
                String messageText = rendered.getRenderedBody();

                SendResult result = voiceProvider.sendVoice(
                        phone,
                        senderNumber,
                        messageText,
                        voiceTemplate.getLanguage(),
                        voiceTemplate.getVoiceName(),
                        voiceTemplate.getVoiceGender()
                );

                if (result.isSuccess()) {
                    sentCount++;
                    log.debug("[Scheduler] Voice call queued to [{}] for activity [{}]", phone, activity.getId());
                } else {
                    bounceCount++;
                    log.warn("[Scheduler] Voice failed for [{}], activity [{}]: {}",
                            phone, activity.getId(), result.getErrorMessage());
                    recordBounce(activity, campaign, phone, result);
                }
            } catch (Exception ex) {
                bounceCount++;
                log.error("[Scheduler] Exception sending Voice to [{}], activity [{}]: {}",
                        phone, activity.getId(), ex.getMessage(), ex);
                recordBounce(activity, campaign, phone, SendResult.failure("Exception: " + ex.getMessage()));
            }
        }

        String providerResponse = String.format("voice: sent=%d, failed=%d, total=%d",
                sentCount, bounceCount, segmentUsers.size());

        CampaignActivityExecutionLog executionLog = CampaignActivityExecutionLog.builder()
                .activity(activity)
                .campaign(campaign)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .status(sentCount > 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .recipientCount(sentCount)
                .providerResponse(providerResponse)
                .errorMessage(bounceCount > 0 ? bounceCount + " recipient(s) failed." : null)
                .build();
        logRepository.save(executionLog);

        activity.setLastExecutionAt(LocalDateTime.now());
        updateActivityAfterExecution(activity, LocalDateTime.now());
        activityRepository.save(activity);
    }

    // -----------------------------------------------------------------------
    // Email execution path (original)
    // -----------------------------------------------------------------------
    private void executeEmailActivity(CampaignActivity activity, Campaign campaign, Template template,
                                       List<SegmentUser> segmentUsers, LocalDateTime startedAt, LocalDateTime now) {
        ITemplateRenderer renderer = rendererFactory.get(template.getRendererType());

        // 3. Resolve email provider — modular via factory, with special Gmail token handling
        IEmailProvider emailProvider = resolveProvider(campaign);

        int sentCount = 0;
        int bounceCount = 0;
        SendResult lastResult = null;

        // Fetch external data if dataSourceUrl is present
        Map<String, Object> externalData = new HashMap<>();
        if (template.getDataSourceUrl() != null && !template.getDataSourceUrl().isBlank()) {
            try {
                Map<String, Object> apiResponse = restTemplate.getForObject(template.getDataSourceUrl(), Map.class);
                if (apiResponse != null) {
                    externalData.putAll(apiResponse);
                }
            } catch (Exception e) {
                log.error("[Scheduler] Failed to fetch external data from URL: {}", template.getDataSourceUrl(), e);
                // Depending on requirements, we could abort here or proceed without data
            }
        }

        // 4. Send one email per recipient
        for (SegmentUser recipient : segmentUsers) {
            String emailId = recipient.getEmailId();

            try {
                // 4a. Build per-recipient template variables
                Map<String, Object> variables = buildVariables(recipient);
                variables.putAll(externalData); // Merge external data

                // 4b. Render subject + body with per-recipient variables
                RenderedTemplate rendered = renderer.render(
                        template.getSubject(),
                        template.getHtmlBody(),
                        variables);

                // 4c. Inject tracking pixel with a unique per-recipient nonce so that
                //     caching proxies (Gmail Image Proxy, Apple Mail, Yahoo) cannot serve
                //     a cached copy — every URL is distinct, forcing a real server hit.
                String nonce = UUID.randomUUID().toString();
                String bodyWithPixel = campaignService.injectTrackingPixel(
                        rendered.getRenderedBody(),
                        campaign.getCampaignCode(),
                        emailId,
                        activity.getId(),
                        nonce);

                String finalBody = shortenLinksInHtml(bodyWithPixel, campaign.getCampaignCode(), recipient.getUserId());

                RenderedTemplate emailContent = new RenderedTemplate(
                        rendered.getRenderedSubject(), finalBody);

                // 4d. Send via provider (single recipient per call)
                lastResult = emailProvider.send(emailContent, Collections.singletonList(emailId), campaign);

                if (lastResult.isSuccess()) {
                    sentCount++;
                    log.debug("[Scheduler] Sent to [{}] for activity [{}]", emailId, activity.getId());
                } else {
                    // 4e. Record bounce
                    bounceCount++;
                    log.warn("[Scheduler] Send failed (bounce) for [{}], activity [{}]: {}",
                            emailId, activity.getId(), lastResult.getErrorMessage());
                    recordBounce(activity, campaign, emailId, lastResult);
                }

            } catch (Exception ex) {
                bounceCount++;
                log.error("[Scheduler] Exception sending to [{}] for activity [{}]: {}",
                        emailId, activity.getId(), ex.getMessage(), ex);
                recordBounce(activity, campaign, emailId,
                        SendResult.failure("Exception: " + ex.getMessage()));
            }
        }

        // 5. Build aggregate result for execution log
        boolean anySuccess = sentCount > 0;
        String providerResponse = String.format("sent=%d, bounced=%d, total=%d",
                sentCount, bounceCount, segmentUsers.size());
        String errorMessage = bounceCount > 0
                ? bounceCount + " recipient(s) bounced. Check email_bounce_events table."
                : null;

        // 6. Persist execution log
        CampaignActivityExecutionLog executionLog = CampaignActivityExecutionLog.builder()
                .activity(activity)
                .campaign(campaign)
                .template(template)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .status(anySuccess ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .recipientCount(sentCount)
                .providerResponse(providerResponse)
                .errorMessage(errorMessage)
                .build();
        logRepository.save(executionLog);

        // 7. Update activity state
        activity.setLastExecutionAt(now);
        updateActivityAfterExecution(activity, now);
        activityRepository.save(activity);
    }

    /**
     * Resolves the IEmailProvider for the given campaign.
     * All providers are retrieved from the factory (extensible — add new
     * providers by implementing IEmailProvider and registering in EmailProviderFactory).
     */
    private IEmailProvider resolveProvider(Campaign campaign) {
        EmailProviderType providerType = campaign.getEmailProviderType() != null
                ? campaign.getEmailProviderType()
                : EmailProviderType.SMTP;

        return emailProviderFactory.get(providerType);
    }

    /**
     * Builds per-recipient template variable map.
     * Standard fields (email, userId, phoneNumber) are always included.
     * Any extra fields stored in SegmentUser.data are merged in so templates can
     * use {{firstName}}, {{plan}}, {{customField}}, etc.
     */
    private Map<String, Object> buildVariables(SegmentUser recipient) {
        Map<String, Object> vars = new HashMap<>();
        // Standard fields
        vars.put("email", recipient.getEmailId());
        if (recipient.getUserId() != null) vars.put("userId", recipient.getUserId());
        if (recipient.getPhoneNumber() != null) vars.put("phoneNumber", recipient.getPhoneNumber());
        // Extra CSV data (firstName, lastName, plan, etc.)
        if (recipient.getData() != null) {
            vars.putAll(recipient.getData());
        }
        return vars;
    }

    private String shortenLinksInHtml(String html, String campaignCode, String userId) {
        if (html == null) return null;
        String baseUrl = shortLinkBaseUrl.endsWith("/") ? shortLinkBaseUrl : shortLinkBaseUrl + "/";
        
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("href\\s*=\\s*\"([^\"]+)\"").matcher(html);
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            // Skip tracking pixels, internal refs, or already shortened links
            if ((originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) 
                 && !originalUrl.contains("/pixel") 
                 && !originalUrl.contains(baseUrl)) {

                String urlWithTrackingString = shortLinkService.shortenLink(originalUrl.toString(), null);
                
                // Build the URL with campaign and user tracking AFTER shortening
                StringBuilder urlWithTracking = new StringBuilder(urlWithTrackingString);
                boolean hasQuery = originalUrl.contains("?");
                if (campaignCode != null) {
                    urlWithTracking.append(hasQuery ? "&" : "?").append("campaign=").append(campaignCode);
                    hasQuery = true;
                }
                if (userId != null) {
                    urlWithTracking.append(hasQuery ? "&" : "?").append("user_id=").append(userId);
                }
                
                matcher.appendReplacement(sb, "href=\"" + Matcher.quoteReplacement(urlWithTracking.toString()) + "\"");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Records a bounce event in the email_bounce_events table.
     */
    private void recordBounce(CampaignActivity activity, Campaign campaign,
                               String emailId, SendResult result) {
        EmailBounceEvent bounce = EmailBounceEvent.builder()
                .activity(activity)
                .campaign(campaign)
                .emailId(emailId)
                .bounceReason(result.getErrorMessage())
                .providerCode(result.getProviderResponse())
                .bouncedAt(LocalDateTime.now())
                .build();
        bounceEventRepository.save(bounce);
    }

    /**
     * After execution: compute next time or mark as COMPLETED for one-time activities.
     */
    private void updateActivityAfterExecution(CampaignActivity activity, LocalDateTime now) {
        if (activity.getScheduleType() == ScheduleType.ONE_TIME) {
            activity.setStatus(ActivityStatus.COMPLETED);
            activity.setNextExecutionAt(null);
            return;
        }

        // Check end-date for recurring
        if (activity.getEndDate() != null && now.toLocalDate().isAfter(activity.getEndDate())) {
            activity.setStatus(ActivityStatus.COMPLETED);
            activity.setNextExecutionAt(null);
            return;
        }

        LocalDateTime next = activityService.computeNextExecution(activity, now);
        activity.setNextExecutionAt(next);
    }
}
