package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CampaignDto;
import com.smartstocks.product.dto.CreateCampaignRequestDto;
import com.smartstocks.product.service.ICampaignService;
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

    @Autowired
    private ICampaignService campaignService;

    @PostMapping
    public ResponseEntity<?> createCampaign(@Valid @RequestBody CreateCampaignRequestDto request) {
        try {
            CampaignDto created = campaignService.createCampaign(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
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
        if (!campaignService.deleteCampaign(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Campaign deleted successfully");
    }

    @PostMapping("/{id}/google-token")
    public ResponseEntity<?> saveGoogleToken(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String token = payload.get("access_token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("access_token is required");
        }
        try {
            campaignService.saveGoogleToken(id, token);
            return ResponseEntity.ok("Token saved");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/google-code")
    public ResponseEntity<?> saveGoogleCode(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String redirectUri = payload.get("redirect_uri");
        if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            return ResponseEntity.badRequest().body("code and redirect_uri are required");
        }
        try {
            campaignService.saveGoogleAuthCode(id, code, redirectUri);
            return ResponseEntity.ok("Code exchanged and tokens saved");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to exchange code: " + e.getMessage());
        }
    }
}
