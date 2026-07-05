package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.service.ICampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    private static final Logger log = LoggerFactory.getLogger(CampaignController.class);

    @Autowired
    private ICampaignService campaignService;

    @PostMapping
    public ResponseEntity<?> createCampaign(@Valid @RequestBody CreateCampaignRequestDto request) {
        log.info("[CampaignController] Creating campaign: name={}", request.getName());
        try {
            CampaignDto created = campaignService.createCampaign(request);
            log.info("[CampaignController] Campaign created with id={}", created.getId());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            log.warn("[CampaignController] Invalid campaign create request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            log.error("[CampaignController] Unexpected error creating campaign: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Unexpected error: " + ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<CampaignDto>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getCampaign(@PathVariable Long id) {
        return campaignService.getCampaignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCampaign(@PathVariable Long id) {
        log.info("[CampaignController] Deleting campaign id={}", id);
        try {
            if (!campaignService.deleteCampaign(id)) {
                log.warn("[CampaignController] Campaign not found for delete id={}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok("Campaign deleted successfully");
        } catch (Exception ex) {
            log.error("[CampaignController] Error deleting campaign id={}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Unexpected error: " + ex.getMessage());
        }
    }

    @PostMapping("/{id}/google-token")
    public ResponseEntity<?> saveGoogleToken(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String token = payload.get("access_token");
        if (token == null || token.isBlank()) {
            log.warn("[CampaignController] saveGoogleToken called without access_token for campaignId={}", id);
            return ResponseEntity.badRequest().body("access_token is required");
        }
        log.info("[CampaignController] Saving Google token for campaignId={}", id);
        try {
            campaignService.saveGoogleToken(id, token);
            return ResponseEntity.ok("Token saved");
        } catch (IllegalArgumentException e) {
            log.warn("[CampaignController] Campaign not found for Google token save id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("[CampaignController] Error saving Google token for campaignId={}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Unexpected error: " + ex.getMessage());
        }
    }

    @PostMapping("/{id}/google-code")
    public ResponseEntity<?> saveGoogleCode(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String redirectUri = payload.get("redirect_uri");
        if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            log.warn("[CampaignController] saveGoogleCode missing required params for campaignId={}", id);
            return ResponseEntity.badRequest().body("code and redirect_uri are required");
        }
        log.info("[CampaignController] Exchanging Google auth code for campaignId={}", id);
        try {
            campaignService.saveGoogleAuthCode(id, code, redirectUri);
            return ResponseEntity.ok("Code exchanged and tokens saved");
        } catch (IllegalArgumentException e) {
            log.warn("[CampaignController] Campaign not found for Google code exchange id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("[CampaignController] Error exchanging Google auth code for campaignId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to exchange code: " + e.getMessage());
        }
    }

    /**
     * Stores the permanent Meta/WhatsApp access token and phone number ID for a campaign.
     * Called after the user manually enters their token from the Meta App Dashboard.
     */
    @PostMapping("/{id}/meta-token")
    public ResponseEntity<?> saveMetaToken(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String token = payload.get("access_token");
        String phoneNumberId = payload.get("phone_number_id");
        if (token == null || token.isBlank()) {
            log.warn("[CampaignController] saveMetaToken missing access_token for campaignId={}", id);
            return ResponseEntity.badRequest().body("access_token is required");
        }
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("[CampaignController] saveMetaToken missing phone_number_id for campaignId={}", id);
            return ResponseEntity.badRequest().body("phone_number_id is required");
        }
        log.info("[CampaignController] Saving Meta token for campaignId={}, phoneNumberId={}", id, phoneNumberId);
        try {
            campaignService.saveMetaToken(id, token, phoneNumberId);
            return ResponseEntity.ok("Meta token saved");
        } catch (IllegalArgumentException e) {
            log.warn("[CampaignController] Campaign not found for Meta token save id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("[CampaignController] Error saving Meta token for campaignId={}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Unexpected error: " + ex.getMessage());
        }
    }

    @PostMapping("/{id}/meta-code")
    public ResponseEntity<?> saveMetaCode(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String redirectUri = payload.get("redirect_uri");
        if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            log.warn("[CampaignController] saveMetaCode missing required params for campaignId={}", id);
            return ResponseEntity.badRequest().body("code and redirect_uri are required");
        }
        log.info("[CampaignController] Exchanging Meta auth code for campaignId={}", id);
        try {
            campaignService.saveMetaAuthCode(id, code, redirectUri);
            return ResponseEntity.ok("Meta auth code exchanged and token saved");
        } catch (IllegalArgumentException e) {
            log.warn("[CampaignController] Campaign not found for Meta code exchange id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("[CampaignController] Error exchanging Meta auth code for campaignId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to exchange Meta code: " + e.getMessage());
        }
    }
}
