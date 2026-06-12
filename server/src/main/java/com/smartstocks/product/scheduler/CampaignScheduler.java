package com.smartstocks.product.scheduler;

import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.CampaignActivityExecutionLogRepository;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.impl.CampaignActivityServiceImpl;
import com.smartstocks.product.service.provider.EmailProviderFactory;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runs every 60 seconds.
 * Picks up all ACTIVE activities whose nextExecutionAt <= now,
 * renders the template, sends via the campaign's email provider, and
 * logs the result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignActivityRepository activityRepository;
    private final CampaignActivityExecutionLogRepository logRepository;
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

        SendResult result;
        try {
            // 1. Resolve renderer
            ITemplateRenderer renderer = rendererFactory.get(template.getRendererType());

            // 2. Render subject + body
            //    Variables would be populated from the campaign/recipient context in a real scenario
            Map<String, Object> variables = Collections.emptyMap();
            RenderedTemplate rendered = renderer.render(template.getSubject(), template.getHtmlBody(), variables);
            String bodyWithPixel = campaignService.injectTrackingPixel(
                    rendered.getRenderedBody(), campaign.getCampaignCode());
            RenderedTemplate emailContent = new RenderedTemplate(rendered.getRenderedSubject(), bodyWithPixel);

            // 3. Resolve email provider (fall back to SMTP if campaign has no provider configured)
            EmailProviderType providerType = campaign.getEmailProviderType() != null
                    ? campaign.getEmailProviderType()
                    : EmailProviderType.SMTP;
            IEmailProvider provider = emailProviderFactory.get(providerType);

            // 4. Send email (recipient list would come from an audience/segment service)
            List<String> recipients = resolveRecipients(campaign);
            result = provider.send(emailContent, recipients);

        } catch (Exception ex) {
            log.error("[Scheduler] Activity [{}] failed: {}", activity.getId(), ex.getMessage(), ex);
            result = SendResult.failure(ex.getMessage());
        }

        // 5. Persist execution log
        CampaignActivityExecutionLog executionLog = CampaignActivityExecutionLog.builder()
                .activity(activity)
                .campaign(campaign)
                .template(template)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .status(result.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .recipientCount(result.getRecipientCount())
                .providerResponse(result.getProviderResponse())
                .errorMessage(result.getErrorMessage())
                .build();
        logRepository.save(executionLog);

        // 6. Update activity state
        activity.setLastExecutionAt(now);
        updateActivityAfterExecution(activity, now);
        activityRepository.save(activity);
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

    /**
     * Placeholder: returns an empty list.
     * Replace with a real look-up against a contacts/segments table.
     */
    private List<String> resolveRecipients(Campaign campaign) {
        // TODO: query recipient list for this campaign from contacts/segments service
        return Collections.emptyList();
    }
}
