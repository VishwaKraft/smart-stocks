package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a named, reusable email event that links a Template to a Campaign.
 * When triggered (via API or UI), the associated template is rendered and sent
 * using the campaign's configured email provider.
 */
@Entity
@Table(name = "campaign_email_events", indexes = {
        @Index(name = "idx_email_event_name", columnList = "eventName", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique slug used in the trigger API URL (e.g. "welcome-email").
     * Must be lowercase alphanumeric with hyphens only.
     */
    @Column(nullable = false, unique = true, length = 128)
    private String eventName;

    /** Human-readable label shown in the UI. */
    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** The campaign whose provider (SMTP / SES / SendGrid) will be used to send the email. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** The email template to render when this event is triggered. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
