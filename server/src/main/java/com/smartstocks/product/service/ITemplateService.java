package com.smartstocks.product.service;

import com.smartstocks.product.dto.CreateTemplateRequestDto;
import com.smartstocks.product.dto.TemplateDto;
import com.smartstocks.product.dto.UpdateTemplateRequestDto;
import com.smartstocks.product.models.Template;

import java.util.List;
import java.util.Optional;

public interface ITemplateService {

    TemplateDto createTemplate(CreateTemplateRequestDto request);

    List<TemplateDto> getAllTemplates();

    List<TemplateDto> getActiveTemplates();

    Optional<TemplateDto> getTemplateById(Long id);

    TemplateDto updateTemplate(Long id, UpdateTemplateRequestDto request);

    boolean deleteTemplate(Long id);   // soft delete

    Optional<Template> findEntityById(Long id);
}
