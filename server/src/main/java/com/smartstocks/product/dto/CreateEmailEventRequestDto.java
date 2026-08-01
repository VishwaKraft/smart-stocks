package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmailEventRequestDto {

    /**
     * URL-safe slug for the event (e.g. "welcome-email").
     * Used in the trigger API: POST /api/email-events/trigger/{eventName}
     */
    @NotBlank(message = "eventName is required")
    @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]{1,126}[a-z0-9]$",
             message = "eventName must be lowercase alphanumeric with hyphens (e.g. welcome-email)")
    private String eventName;

    @NotBlank(message = "displayName is required")
    @Size(max = 255)
    private String displayName;

    @Size(max = 2048)
    private String description;

    @NotNull(message = "campaignId is required")
    private Long campaignId;

    @NotNull(message = "templateId is required")
    private Long templateId;
}
