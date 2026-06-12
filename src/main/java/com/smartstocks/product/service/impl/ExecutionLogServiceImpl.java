package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.ExecutionLogDto;
import com.smartstocks.product.models.CampaignActivityExecutionLog;
import com.smartstocks.product.repository.CampaignActivityExecutionLogRepository;
import com.smartstocks.product.service.IExecutionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionLogServiceImpl implements IExecutionLogService {

    private final CampaignActivityExecutionLogRepository logRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionLogDto> getLogsForActivity(Long activityId) {
        return logRepository.findAllByActivityIdOrderByCreatedAtDesc(activityId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ExecutionLogDto toDto(CampaignActivityExecutionLog log) {
        return ExecutionLogDto.builder()
                .id(log.getId())
                .activityId(log.getActivity().getId())
                .campaignId(log.getCampaign().getId())
                .campaignName(log.getCampaign().getName())
                .templateId(log.getTemplate().getId())
                .templateName(log.getTemplate().getName())
                .startedAt(log.getStartedAt())
                .completedAt(log.getCompletedAt())
                .status(log.getStatus())
                .recipientCount(log.getRecipientCount())
                .providerResponse(log.getProviderResponse())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
