package com.smartstocks.product.dto;

import com.smartstocks.product.models.CampaignType;
import com.smartstocks.product.models.EmailProviderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequestDto {

    @NotBlank(message = "name cannot be empty")
    private String name;

    private String description;

    private String campaignCode;

    private CampaignType campaignType;

    private String whatsappSenderNumber;

    private String infobipSenderNumber;

    /** Optional: email delivery provider for this campaign */
    private EmailProviderType emailProviderType;
}

