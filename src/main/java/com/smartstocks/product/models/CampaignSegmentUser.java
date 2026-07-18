package com.smartstocks.product.models;

import com.smartstocks.product.converters.MapToJsonConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * A denormalized snapshot of segment users for a specific campaign activity.
 * Populated during the GENERATE stage before campaign execution.
 * For VOICE campaigns, also tracks whether the Infobip person record was created.
 */
@Entity
@Table(name = "campaign_segment_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSegmentUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private CampaignActivity activity;

    @Column(name = "email_id")
    private String emailId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Extra per-recipient data (any additional CSV columns).
     * Available as template variables during message rendering.
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "data", columnDefinition = "TEXT")
    private Map<String, Object> data;

    /**
     * For VOICE campaigns: tracks whether this recipient has been registered
     * as a person in Infobip via the People API.
     */
    @Column(name = "infobip_person_created")
    private Boolean infobipPersonCreated = false;

    /** Infobip person ID returned after creation, for reference. */
    @Column(name = "infobip_person_id", length = 100)
    private String infobipPersonId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
