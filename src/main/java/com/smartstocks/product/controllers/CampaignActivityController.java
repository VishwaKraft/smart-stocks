package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CampaignActivityDto;
import com.smartstocks.product.dto.CreateActivityRequestDto;
import com.smartstocks.product.dto.ExecutionLogDto;
import com.smartstocks.product.service.ICampaignActivityService;
import com.smartstocks.product.service.IExecutionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class CampaignActivityController {

    @Autowired
    private ICampaignActivityService activityService;

    @Autowired
    private IExecutionLogService executionLogService;

    /**
     * POST /api/activities
     */
    @PostMapping
    public ResponseEntity<?> createActivity(@Valid @RequestBody CreateActivityRequestDto request) {
        try {
            CampaignActivityDto created = activityService.createActivity(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * GET /api/activities
     * Optional ?campaignId=X to filter by campaign.
     */
    @GetMapping
    public ResponseEntity<List<CampaignActivityDto>> getAllActivities(
            @RequestParam(required = false) Long campaignId) {
        List<CampaignActivityDto> activities = campaignId != null
                ? activityService.getActivitiesByCampaign(campaignId)
                : activityService.getAllActivities();
        return ResponseEntity.ok(activities);
    }

    /**
     * GET /api/activities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignActivityDto> getActivity(@PathVariable Long id) {
        return activityService.getActivityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/activities/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody CreateActivityRequestDto request) {
        try {
            CampaignActivityDto updated = activityService.updateActivity(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * DELETE /api/activities/{id}
     * Logical delete: sets status = CANCELLED.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteActivity(@PathVariable Long id) {
        if (!activityService.deleteActivity(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Activity cancelled successfully");
    }

    /**
     * GET /api/activities/{id}/executions
     * Returns audit execution logs for a given activity.
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ExecutionLogDto>> getExecutionLogs(@PathVariable Long id) {
        return ResponseEntity.ok(executionLogService.getLogsForActivity(id));
    }
}
