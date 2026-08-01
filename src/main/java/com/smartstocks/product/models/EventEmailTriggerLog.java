package com.smartstocks.product.models;

import com.smartstocks.product.converters.MapToJsonConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Audit/tracking record for every time an EmailEvent is triggered.
 * Stored in event_email_trigger_logs for tracking and audit purposes.
 */
@Entity
@Table(name = "event_email_trigger_logs", indexes = {
        @Index(name = "idx_trigger_log_event_name", columnList = "eventName"),
        @Index(name = "idx_trigger_log_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventEmailTriggerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The slug of the event that was triggered. */
    @Column(nullable = false, length = 128)
    private String eventName;

    /** FK to the EmailEvent (kept as simple Long to avoid lazy-load issues in async contexts). */
    @Column(nullable = false)
    private Long emailEventId;

    /** Campaign ID used for this trigger. */
    @Column(nullable = false)
    private Long campaignId;

    /** Template ID rendered for this trigger. */
    @Column(nullable = false)
    private Long templateId;

    /** JSON-serialised list of recipient addresses. */
    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> variables;

    /** Comma-separated recipient emails (kept plain for easy querying). */
    @Column(columnDefinition = "TEXT")
    private String recipients;

    /** Number of recipients in this trigger call. */
    private Integer recipientCount;

    /** Whether the email provider reported success. */
    @Column(nullable = false)
    private Boolean success;

    /** Raw provider response or error message. */
    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
