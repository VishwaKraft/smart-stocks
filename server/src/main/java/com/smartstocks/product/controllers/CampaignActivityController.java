package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CampaignActivityDto;
import com.smartstocks.product.dto.CreateActivityRequestDto;
import com.smartstocks.product.dto.ExecutionLogDto;
import com.smartstocks.product.service.ICampaignActivityService;
import com.smartstocks.product.service.IExecutionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
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
     * Creates an activity with status=NEW and isDeleted=false.
     * Use /activate or /pause endpoints to change state.
     */
    @PostMapping
    public ResponseEntity<?> createActivity(@Valid @RequestBody CreateActivityRequestDto request) {
        try {
            CampaignActivityDto created = activityService.createActivity(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            log.error("[CampaignActivityController] Bad Request when creating activity: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * GET /api/activities
     * Optional ?campaignId=X to filter by campaign. Soft-deleted activities are hidden.
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
            log.error("[CampaignActivityController] Bad Request when updating activity {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * DELETE /api/activities/{id}
     * Soft-deletes the activity (isDeleted=true). It will no longer appear in lists
     * or be picked up by the scheduler.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteActivity(@PathVariable Long id) {
        if (!activityService.deleteActivity(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Activity deleted successfully");
    }

    /**
     * POST /api/activities/{id}/activate
     * Transitions status to ACTIVE. The scheduler will pick this up on the next tick.
     * Requires the activity to have status=READY (after a successful test fire).
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateActivity(@PathVariable Long id) {
        try {
            CampaignActivityDto dto = activityService.activateActivity(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("[CampaignActivityController] Bad Request when activating activity {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * POST /api/activities/{id}/pause
     * Transitions status to PAUSED. The scheduler will skip the activity until re-activated.
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseActivity(@PathVariable Long id) {
        try {
            CampaignActivityDto dto = activityService.pauseActivity(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("[CampaignActivityController] Bad Request when pausing activity {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * GET /api/activities/{id}/executions
     * Returns audit execution logs for a given activity.
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ExecutionLogDto>> getExecutionLogs(@PathVariable Long id) {
        return ResponseEntity.ok(executionLogService.getLogsForActivity(id));
    }

    /**
     * POST /api/activities/{id}/test-trigger
     * Sends a test email. On success, transitions status from NEW → READY.
     */
    @PostMapping("/{id}/test-trigger")
    public ResponseEntity<?> testTrigger(@PathVariable Long id, @RequestBody(required = false) List<String> emailIds) {
        try {
            activityService.testTrigger(id, emailIds);
            return ResponseEntity.ok("Test trigger executed successfully. Activity is now READY.");
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException ex) {
            log.error("[CampaignActivityController] Bad Request when test triggering activity {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Test failed: " + ex.getMessage());
        }
    }

    /**
     * POST /api/activities/{id}/clone
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<?> cloneActivity(@PathVariable Long id, @RequestParam String newName) {
        try {
            CampaignActivityDto cloned = activityService.cloneActivity(id, newName);
            return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
        } catch (IllegalArgumentException ex) {
            log.error("[CampaignActivityController] Bad Request when cloning activity {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * POST /api/activities/{id}/generate
     * Triggers generation of recipients and moves status from GENERATING to NEW.
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<?> generateActivityData(@PathVariable Long id) {
        try {
            CampaignActivityDto generated = activityService.generateActivityData(id);
            return ResponseEntity.ok(generated);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("[CampaignActivityController] Bad Request when generating activity data {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}

