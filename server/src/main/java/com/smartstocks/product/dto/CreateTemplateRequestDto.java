package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequestDto {

    @NotBlank(message = "Template name cannot be empty")
    private String name;

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @NotBlank(message = "HTML body cannot be empty")
    private String htmlBody;
}
