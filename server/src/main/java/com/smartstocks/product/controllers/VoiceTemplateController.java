package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.VoiceTemplateDto;
import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.models.VoiceTemplate;
import com.smartstocks.product.repository.CampaignRepository;
import com.smartstocks.product.repository.VoiceTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Infobip Voice (TTS) template management.
 * Templates store the message text, language, and voice settings used
 * when firing voice campaigns via Infobip's /tts/3/advanced API.
 */
@Slf4j
@RestController
@RequestMapping("/api/voice/templates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VoiceTemplateController {

    private final VoiceTemplateRepository voiceTemplateRepository;
    private final CampaignRepository campaignRepository;

    /** GET /api/voice/templates — list all active voice templates, optionally filtered by campaign */
    @GetMapping
    public ResponseEntity<List<VoiceTemplateDto>> getAllTemplates(
            @RequestParam(value = "campaignId", required = false) Long campaignId) {
        List<VoiceTemplate> templates = (campaignId != null)
                ? voiceTemplateRepository.findByCampaignIdAndIsActiveTrue(campaignId)
                : voiceTemplateRepository.findByIsActiveTrue();
        return ResponseEntity.ok(templates.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /** GET /api/voice/templates/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<VoiceTemplateDto> getTemplate(@PathVariable Long id) {
        return voiceTemplateRepository.findById(id)
                .map(t -> ResponseEntity.ok(toDto(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/voice/templates — create a new voice template */
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody VoiceTemplateDto request) {
        try {
            VoiceTemplate template = new VoiceTemplate();
            applyRequest(template, request);
            VoiceTemplate saved = voiceTemplateRepository.save(template);
            log.info("[VoiceTemplateController] Created voice template id={}, name={}", saved.getId(), saved.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** PUT /api/voice/templates/{id} — update an existing voice template */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @Valid @RequestBody VoiceTemplateDto request) {
        return voiceTemplateRepository.findById(id).map(template -> {
            try {
                applyRequest(template, request);
                VoiceTemplate saved = voiceTemplateRepository.save(template);
                log.info("[VoiceTemplateController] Updated voice template id={}", saved.getId());
                return ResponseEntity.ok(toDto(saved));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().<VoiceTemplateDto>body(null);
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/voice/templates/{id} — soft-delete (marks isActive=false) */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
        return voiceTemplateRepository.findById(id).map(template -> {
            template.setIsActive(false);
            voiceTemplateRepository.save(template);
            log.info("[VoiceTemplateController] Soft-deleted voice template id={}", id);
            return ResponseEntity.ok("Voice template deleted successfully");
        }).orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------

    private void applyRequest(VoiceTemplate template, VoiceTemplateDto request) {
        template.setName(request.getName());
        template.setMessageText(request.getMessageText());
        template.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en");
        template.setVoiceName(request.getVoiceName() != null ? request.getVoiceName() : "Joanna");
        template.setVoiceGender(request.getVoiceGender() != null ? request.getVoiceGender() : "female");
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + request.getCampaignId()));
            template.setCampaign(campaign);
        } else {
            template.setCampaign(null);
        }
    }

    private VoiceTemplateDto toDto(VoiceTemplate t) {
        return VoiceTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .messageText(t.getMessageText())
                .language(t.getLanguage())
                .voiceName(t.getVoiceName())
                .voiceGender(t.getVoiceGender())
                .campaignId(t.getCampaign() != null ? t.getCampaign().getId() : null)
                .campaignName(t.getCampaign() != null ? t.getCampaign().getName() : null)
                .isActive(t.getIsActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
