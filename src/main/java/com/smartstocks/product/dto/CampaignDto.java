package com.smartstocks.product.dto;

import com.smartstocks.product.models.CampaignType;
import com.smartstocks.product.models.EmailProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignDto {

    private Long id;
    private String campaignCode;
    private String name;
    private String description;
    private CampaignType campaignType;
    private String whatsappSenderNumber;
    private EmailProviderType emailProviderType;
    private String trackingPixelUrl;
    private long openCount;
    private LocalDateTime createdAt;
}

