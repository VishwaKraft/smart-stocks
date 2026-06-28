package com.smartstocks.product.scheduler;

import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.CampaignActivityExecutionLogRepository;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.repository.EmailBounceEventRepository;
import com.smartstocks.product.repository.SegmentUserRepository;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.impl.CampaignActivityServiceImpl;
import com.smartstocks.product.service.provider.EmailProviderFactory;
import com.smartstocks.product.service.provider.GmailProvider;
import com.smartstocks.product.service.provider.IEmailProvider;
import com.smartstocks.product.service.provider.SendResult;
import com.smartstocks.product.service.renderer.ITemplateRenderer;
import com.smartstocks.product.service.renderer.RenderedTemplate;
import com.smartstocks.product.service.renderer.TemplateRendererFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    /**
     * Trigger every minute (cron: second=0 of every minute).
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processDueActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignActivity> dueActivities = activityRepository.findDueActivities(now);

        if (dueActivities.isEmpty()) {
            return;
        }

        log.info("[Scheduler] Processing {} due activity(-ies) at {}", dueActivities.size(), now);

        for (CampaignActivity activity : dueActivities) {
            executeActivity(activity, now);
        }
    }

    // -----------------------------------------------------------------------
    private void executeActivity(CampaignActivity activity, LocalDateTime now) {
        LocalDateTime startedAt = LocalDateTime.now();
        Campaign campaign = activity.getCampaign();
        Template template = activity.getTemplate();

        log.info("[Scheduler] Executing activity [{}] for campaign [{}] using template [{}]",
                activity.getId(), campaign.getName(), template.getName());

        // 1. Load all segment users for this activity's segment
        List<SegmentUser> segmentUsers = (activity.getSegment() != null)
                ? segmentUserRepository.findBySegmentId(activity.getSegment().getId())
                : Collections.emptyList();

        if (segmentUsers.isEmpty()) {
            log.warn("[Scheduler] Activity [{}] has no recipients in segment. Skipping.", activity.getId());
            return;
        }

        // 2. Resolve renderer
        ITemplateRenderer renderer = rendererFactory.get(template.getRendererType());

        // 3. Resolve email provider — modular via factory, with special Gmail token handling
        IEmailProvider emailProvider = resolveProvider(campaign);

        int sentCount = 0;
        int bounceCount = 0;
        SendResult lastResult = null;

        // 4. Send one email per recipient
        for (SegmentUser recipient : segmentUsers) {
            String emailId = recipient.getEmailId();

            try {
                // 4a. Build per-recipient template variables
                Map<String, Object> variables = buildVariables(recipient);

                // 4b. Render subject + body with per-recipient variables
                RenderedTemplate rendered = renderer.render(
                        template.getSubject(),
                        template.getHtmlBody(),
                        variables);

                // 4c. Inject tracking pixel with emailId + activityId
                String bodyWithPixel = campaignService.injectTrackingPixel(
                        rendered.getRenderedBody(),
                        campaign.getCampaignCode(),
                        emailId,
                        activity.getId());

                RenderedTemplate emailContent = new RenderedTemplate(
                        rendered.getRenderedSubject(), bodyWithPixel);

                // 4d. Send via provider (single recipient per call)
                lastResult = emailProvider.send(emailContent, Collections.singletonList(emailId));

                // Handle Gmail-specific auth token refresh
                if (lastResult.isAuthError() && campaign.getEmailProviderType() == EmailProviderType.GMAIL) {
                    log.warn("[Scheduler] Gmail auth error for activity [{}], refreshing token.", activity.getId());
                    String newToken = campaignService.refreshGoogleAccessToken(campaign.getId());
                    emailProvider = new GmailProvider(newToken);
                    lastResult = emailProvider.send(emailContent, Collections.singletonList(emailId));
                }

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
     * Gmail requires a live access token so it is constructed directly;
     * all other providers are retrieved from the factory (extensible — add new
     * providers by implementing IEmailProvider and registering in EmailProviderFactory).
     */
    private IEmailProvider resolveProvider(Campaign campaign) {
        EmailProviderType providerType = campaign.getEmailProviderType() != null
                ? campaign.getEmailProviderType()
                : EmailProviderType.SMTP;

        if (providerType == EmailProviderType.GMAIL) {
            String accessToken = campaign.getGoogleAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalStateException("Gmail is not authorized for this campaign.");
            }
            return new GmailProvider(accessToken);
        }

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

