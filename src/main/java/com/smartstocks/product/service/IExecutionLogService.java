package com.smartstocks.product.service;

import com.smartstocks.product.dto.ExecutionLogDto;

import java.util.List;

public interface IExecutionLogService {

    List<ExecutionLogDto> getLogsForActivity(Long activityId);
}
