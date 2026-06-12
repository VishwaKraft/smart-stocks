package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CreateTemplateRequestDto;
import com.smartstocks.product.dto.TemplateDto;
import com.smartstocks.product.dto.UpdateTemplateRequestDto;
import com.smartstocks.product.service.ITemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {

    @Autowired
    private ITemplateService templateService;

    /**
     * POST /api/templates
     * Create a new email template.
     */
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody CreateTemplateRequestDto request) {
        try {
            TemplateDto created = templateService.createTemplate(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * GET /api/templates
     * Returns all active templates.
     */
    @GetMapping
    public ResponseEntity<List<TemplateDto>> getAllTemplates(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<TemplateDto> templates = includeInactive
                ? templateService.getAllTemplates()
                : templateService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * GET /api/templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateDto> getTemplate(@PathVariable Long id) {
        return templateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequestDto request) {
        try {
            TemplateDto updated = templateService.updateTemplate(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * DELETE /api/templates/{id}
     * Soft-deletes: sets isActive = false.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
        if (!templateService.deleteTemplate(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Template deactivated successfully");
    }
}
