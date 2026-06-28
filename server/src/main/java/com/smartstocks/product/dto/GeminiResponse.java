package com.smartstocks.product.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class GeminiResponse {
    private final String message;
    private final String functionName;
    private final String functionArgs;
    private final boolean hasErrors;
    private final int statusCode;
    private final String model;
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    private final String finishReason;
    private final long createdTimestamp;
    private final JsonNode parsedContent;
}