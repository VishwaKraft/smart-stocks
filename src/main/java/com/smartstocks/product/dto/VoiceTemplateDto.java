package com.smartstocks.product.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceTemplateDto {

    private Long id;

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Message text is required")
    private String messageText;

    /** BCP-47 / Infobip language code, e.g. "en", "hi". Defaults to "en". */
    private String language;

    /** Infobip voice name, e.g. "Joanna", "Matthew". Defaults to "Joanna". */
    private String voiceName;

    /** "female" or "male". Defaults to "female". */
    private String voiceGender;

    /** Optional: tie to a specific campaign. */
    private Long campaignId;
    private String campaignName;
    
    private String dataSourceUrl;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
