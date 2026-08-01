package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CreateEmailEventRequestDto;
import com.smartstocks.product.dto.EmailEventDto;
import com.smartstocks.product.dto.TriggerEventEmailRequestDto;
import com.smartstocks.product.dto.TriggerEventEmailResponseDto;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.models.EmailEvent;
import com.smartstocks.product.models.EventEmailTriggerLog;
import com.smartstocks.product.models.Template;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.EmailEventRepository;
import com.smartstocks.product.repository.EventEmailTriggerLogRepository;
import com.smartstocks.product.repository.TemplateRepository;
import com.smartstocks.product.service.IEmailEventService;
import com.smartstocks.product.service.provider.EmailProviderFactory;
import com.smartstocks.product.service.provider.IEmailProvider;
import com.smartstocks.product.service.provider.SendResult;
import com.smartstocks.product.service.renderer.ITemplateRenderer;
import com.smartstocks.product.service.renderer.RenderedTemplate;
import com.smartstocks.product.service.renderer.TemplateRendererFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventServiceImpl implements IEmailEventService {

    private final EmailEventRepository emailEventRepository;
    private final EventEmailTriggerLogRepository triggerLogRepository;
    private final CampaignRepository campaignRepository;
    private final TemplateRepository templateRepository;
    private final EmailProviderFactory emailProviderFactory;
    private final TemplateRendererFactory templateRendererFactory;

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public EmailEventDto createEmailEvent(CreateEmailEventRequestDto request) {
        if (emailEventRepository.existsByEventName(request.getEventName())) {
            throw new IllegalArgumentException(
                    "An event with name '" + request.getEventName() + "' already exists");
        }

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign not found: " + request.getCampaignId()));

        Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Template not found: " + request.getTemplateId()));

        EmailEvent event = new EmailEvent();
        event.setEventName(request.getEventName().trim().toLowerCase());
        event.setDisplayName(request.getDisplayName().trim());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setCampaign(campaign);
        event.setTemplate(template);
        event.setIsActive(true);

        EmailEvent saved = emailEventRepository.save(event);
        log.info("[EmailEventService] Created email event '{}' (id={}) -> campaign={}, template={}",
                saved.getEventName(), saved.getId(), campaign.getId(), template.getId());

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailEventDto> listAllEmailEvents() {
        return emailEventRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailEventDto> listActiveEmailEvents() {
        return emailEventRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailEventDto> getById(Long id) {
        return emailEventRepository.findById(id).map(this::toDto);
    }

    @Override
    @Transactional
    public boolean deleteEmailEvent(Long id) {
        return emailEventRepository.findById(id).map(event -> {
            event.setIsActive(false);
            emailEventRepository.save(event);
            log.info("[EmailEventService] Soft-deleted email event '{}' (id={})", event.getEventName(), id);
            return true;
        }).orElse(false);
    }

    // -----------------------------------------------------------------------
    // TRIGGER
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public TriggerEventEmailResponseDto triggerEvent(String eventName, TriggerEventEmailRequestDto request) {
        // 1. Resolve event
        EmailEvent event = emailEventRepository.findByEventName(eventName)
                .orElseThrow(() -> new IllegalArgumentException("Email event not found: " + eventName));

        if (!event.getIsActive()) {
            throw new IllegalStateException("Email event '" + eventName + "' is inactive and cannot be triggered");
        }

        Template template = event.getTemplate();
        Campaign campaign = event.getCampaign();

        List<String> recipients = request.getRecipients();

        // 2. Render the template with provided variables
        ITemplateRenderer renderer = templateRendererFactory.get(template.getRendererType());
        RenderedTemplate rendered = renderer.render(
                template.getSubject(),
                template.getHtmlBody(),
                request.getVariables() != null ? request.getVariables() : Collections.emptyMap());

        // 3. Resolve email provider from campaign configuration
        if (campaign.getEmailProviderType() == null) {
            String errMsg = "Campaign '" + campaign.getName() + "' has no email provider configured";
            log.error("[EmailEventService] {}", errMsg);
            EventEmailTriggerLog failLog = buildTriggerLog(event, recipients, request, false, null, errMsg);
            EventEmailTriggerLog savedLog = triggerLogRepository.save(failLog);
            return TriggerEventEmailResponseDto.builder()
                    .eventName(eventName)
                    .displayName(event.getDisplayName())
                    .success(false)
                    .recipientCount(0)
                    .recipients(recipients)
                    .errorMessage(errMsg)
                    .triggerLogId(savedLog.getId())
                    .triggeredAt(LocalDateTime.now())
                    .build();
        }

        IEmailProvider provider = emailProviderFactory.get(campaign.getEmailProviderType());

        // 4. Send emails
        SendResult result;
        try {
            result = provider.send(rendered, recipients);
        } catch (Exception ex) {
            log.error("[EmailEventService] Provider send failed for event '{}': {}", eventName, ex.getMessage(), ex);
            result = SendResult.failure(ex.getMessage());
        }

        // 5. Persist audit log
        EventEmailTriggerLog triggerLog = buildTriggerLog(event, recipients, request,
                result.isSuccess(), result.getProviderResponse(), result.getErrorMessage());
        EventEmailTriggerLog savedLog = triggerLogRepository.save(triggerLog);

        log.info("[EmailEventService] Event '{}' triggered -> success={}, recipients={}, logId={}",
                eventName, result.isSuccess(), recipients.size(), savedLog.getId());

        return TriggerEventEmailResponseDto.builder()
                .eventName(eventName)
                .displayName(event.getDisplayName())
                .success(result.isSuccess())
                .recipientCount(result.getRecipientCount())
                .recipients(recipients)
                .providerResponse(result.getProviderResponse())
                .errorMessage(result.getErrorMessage())
                .triggerLogId(savedLog.getId())
                .triggeredAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private EventEmailTriggerLog buildTriggerLog(EmailEvent event, List<String> recipients,
                                                  TriggerEventEmailRequestDto request,
                                                  boolean success, String providerResponse, String errorMessage) {
        EventEmailTriggerLog log = new EventEmailTriggerLog();
        log.setEventName(event.getEventName());
        log.setEmailEventId(event.getId());
        log.setCampaignId(event.getCampaign().getId());
        log.setTemplateId(event.getTemplate().getId());
        log.setVariables(request.getVariables());
        log.setRecipients(String.join(",", recipients));
        log.setRecipientCount(recipients.size());
        log.setSuccess(success);
        log.setProviderResponse(providerResponse);
        log.setErrorMessage(errorMessage);
        return log;
    }

    private EmailEventDto toDto(EmailEvent event) {
        long total = triggerLogRepository.countByEmailEventId(event.getId());
        long success = triggerLogRepository.countByEmailEventIdAndSuccessTrue(event.getId());

        return EmailEventDto.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .displayName(event.getDisplayName())
                .description(event.getDescription())
                .campaignId(event.getCampaign().getId())
                .campaignName(event.getCampaign().getName())
                .campaignCode(event.getCampaign().getCampaignCode())
                .emailProviderType(event.getCampaign().getEmailProviderType() != null
                        ? event.getCampaign().getEmailProviderType().name() : null)
                .templateId(event.getTemplate().getId())
                .templateName(event.getTemplate().getName())
                .templateSubject(event.getTemplate().getSubject())
                .isActive(event.getIsActive())
                .triggerCount(total)
                .successCount(success)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
