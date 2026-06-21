package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.CreateTemplateRequestDto;
import com.smartstocks.product.dto.TemplateDto;
import com.smartstocks.product.dto.UpdateTemplateRequestDto;
import com.smartstocks.product.models.RendererType;
import com.smartstocks.product.models.Template;
import com.smartstocks.product.repository.TemplateRepository;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ITemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements ITemplateService {

    private final TemplateRepository templateRepository;
    private final CampaignEventLogger eventLogger;

    @Override
    @Transactional
    public TemplateDto createTemplate(CreateTemplateRequestDto request) {
        if (templateRepository.existsByName(request.getName().trim())) {
            throw new IllegalArgumentException("A template named '" + request.getName() + "' already exists");
        }

        Template template = new Template();
        template.setName(request.getName().trim());
        template.setSubject(request.getSubject().trim());
        template.setHtmlBody(request.getHtmlBody());
        template.setRendererType(RendererType.DEFAULT);
        template.setIsActive(true);

        Template saved = templateRepository.save(template);

        Map<String, Object> info = new HashMap<>();
        info.put("templateId", saved.getId());
        info.put("templateName", saved.getName());
        eventLogger.log("TEMPLATE_CREATED", info);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> getActiveTemplates() {
        return templateRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TemplateDto> getTemplateById(Long id) {
        return templateRepository.findById(id).map(this::toDto);
    }

    @Override
    @Transactional
    public TemplateDto updateTemplate(Long id, UpdateTemplateRequestDto request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        // Name uniqueness: allow same name on same entity
        if (!template.getName().equals(request.getName().trim())) {
            if (templateRepository.existsByName(request.getName().trim())) {
                throw new IllegalArgumentException("A template named '" + request.getName() + "' already exists");
            }
            template.setName(request.getName().trim());
        }

        template.setSubject(request.getSubject().trim());
        template.setHtmlBody(request.getHtmlBody());
        template.setRendererType(RendererType.DEFAULT);

        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }

        Template saved = templateRepository.save(template);

        Map<String, Object> info = new HashMap<>();
        info.put("templateId", saved.getId());
        info.put("templateName", saved.getName());
        eventLogger.log("TEMPLATE_UPDATED", info);

        return toDto(saved);
    }

    @Override
    @Transactional
    public boolean deleteTemplate(Long id) {
        return templateRepository.findById(id).map(template -> {
            template.setIsActive(false);   // soft delete
            templateRepository.save(template);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Template> findEntityById(Long id) {
        return templateRepository.findById(id);
    }

    // -----------------------------------------------------------------------
    private TemplateDto toDto(Template t) {
        return TemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .subject(t.getSubject())
                .htmlBody(t.getHtmlBody())
                .rendererType(t.getRendererType())
                .isActive(t.getIsActive())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
