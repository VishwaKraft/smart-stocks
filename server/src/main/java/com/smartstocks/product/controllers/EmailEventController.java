package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.CreateEmailEventRequestDto;
import com.smartstocks.product.dto.EmailEventDto;
import com.smartstocks.product.dto.TriggerEventEmailRequestDto;
import com.smartstocks.product.dto.TriggerEventEmailResponseDto;
import com.smartstocks.product.service.IEmailEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for managing and triggering named email events.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   POST   /api/email-events                         — Create an event (link template + campaign)
 *   GET    /api/email-events                         — List all events
 *   GET    /api/email-events/{id}                    — Get a single event
 *   DELETE /api/email-events/{id}                    — Soft-delete an event
 *   POST   /api/email-events/trigger/{eventName}     — Trigger event-driven email (public API)
 * </pre>
 */
@RestController
@RequestMapping("/api/email-events")
@CrossOrigin(origins = "*")
public class EmailEventController {

    @Autowired
    private IEmailEventService emailEventService;

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    /**
     * POST /api/email-events
     * Associate a template with a campaign to create a named email event.
     */
    @PostMapping
    public ResponseEntity<?> createEmailEvent(@Valid @RequestBody CreateEmailEventRequestDto request) {
        try {
            EmailEventDto created = emailEventService.createEmailEvent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * GET /api/email-events
     * Returns all email events. Pass ?includeInactive=true to include soft-deleted ones.
     */
    @GetMapping
    public ResponseEntity<List<EmailEventDto>> listEmailEvents(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<EmailEventDto> events = includeInactive
                ? emailEventService.listAllEmailEvents()
                : emailEventService.listActiveEmailEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/email-events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmailEvent(@PathVariable Long id) {
        return emailEventService.getById(id)
                .map(dto -> ResponseEntity.ok((Object) dto))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/email-events/{id}
     * Soft-delete: sets isActive = false.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmailEvent(@PathVariable Long id) {
        if (!emailEventService.deleteEmailEvent(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Email event deactivated successfully");
    }

    // -----------------------------------------------------------------------
    // TRIGGER — event-driven email API
    // -----------------------------------------------------------------------

    /**
     * POST /api/email-events/trigger/{eventName}
     *
     * <p>Triggers an event-driven email send. Downstream systems call this endpoint
     * when a business event occurs (e.g., user registration, order confirmation).
     *
     * <p>Request body:
     * <pre>
     * {
     *   "recipients": ["alice@example.com"],
     *   "variables":  { "name": "Alice", "plan": "Pro" }
     * }
     * </pre>
     *
     * <p>The template is rendered with the provided variables, then sent via
     * the campaign's configured email provider. Every call is persisted to
     * the event_email_trigger_logs audit table.
     */
    @PostMapping("/trigger/{eventName}")
    public ResponseEntity<?> triggerEvent(
            @PathVariable String eventName,
            @Valid @RequestBody TriggerEventEmailRequestDto request) {
        try {
            TriggerEventEmailResponseDto response = emailEventService.triggerEvent(eventName, request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}
