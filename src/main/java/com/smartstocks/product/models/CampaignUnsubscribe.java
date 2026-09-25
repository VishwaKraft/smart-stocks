package com.smartstocks.product.models;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks email addresses that have unsubscribed from a specific campaign.
 * When a recipient clicks the unsubscribe link in any activity email for a
 * campaign, an entry is recorded here.
 *
 * <p>During the segment generation phase (GENERATE stage) and at send time,
 * any email present in this table for the relevant campaign is filtered out
 * so unsubscribed recipients never receive further messages.
 */
@Entity
@Table(
    name = "campaign_unsubscribes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_campaign_unsubscribe",
        columnNames = {"campaign_id", "email_id"}
    ),
    indexes = {
        @Index(name = "idx_unsub_campaign_email", columnList = "campaign_id, email_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignUnsubscribe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** Email address of the recipient who unsubscribed. */
    @Column(name = "email_id", nullable = false, length = 255)
    private String emailId;

    /**
     * Optional: the activity from whose email the unsubscribe link was clicked.
     * Null when unsubscribing directly without an activity context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private CampaignActivity activity;

    /** Client IP at the time of unsubscribe (for audit). */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "unsubscribed_at", updatable = false)
    private LocalDateTime unsubscribedAt;
}
