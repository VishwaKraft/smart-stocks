package com.smartstocks.product.dto;

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
}
