package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Records email addresses that bounced during a campaign activity execution.
 * A bounce is recorded when the email provider rejects or fails to deliver
 * to a specific recipient email address.
 */
@Entity
@Table(name = "email_bounce_events", indexes = {
        @Index(name = "idx_bounce_activity", columnList = "activity_id"),
        @Index(name = "idx_bounce_email", columnList = "email_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailBounceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The activity that attempted the send */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private CampaignActivity activity;

    /** The campaign this activity belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    /** The email address that bounced */
    @Column(name = "email_id", nullable = false, length = 255)
    private String emailId;

    /** Provider-level error message or bounce reason */
    @Column(columnDefinition = "TEXT")
    private String bounceReason;

    /** Provider response code/status if available */
    @Column(length = 100)
    private String providerCode;

    /** When the bounce was detected */
    @Column(nullable = false)
    private LocalDateTime bouncedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
