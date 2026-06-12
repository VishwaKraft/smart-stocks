package com.smartstocks.product.service.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SendResult {

    private final boolean success;
    private final int recipientCount;
    private final String providerResponse;
    private final String errorMessage;

    public static SendResult ok(int recipientCount, String providerResponse) {
        return SendResult.builder()
                .success(true)
                .recipientCount(recipientCount)
                .providerResponse(providerResponse)
                .build();
    }

    public static SendResult failure(String errorMessage) {
        return SendResult.builder()
                .success(false)
                .recipientCount(0)
                .errorMessage(errorMessage)
                .build();
    }
}
