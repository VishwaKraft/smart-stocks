package com.smartstocks.product.dto;

import com.smartstocks.product.models.RendererType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDto {

    private Long id;
    private String name;
    private String subject;
    private String htmlBody;
    private RendererType rendererType;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
