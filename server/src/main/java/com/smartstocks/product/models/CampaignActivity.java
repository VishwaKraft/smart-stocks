package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "campaign_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(length = 255)
    private String activityName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecurrenceType recurrenceType;

    /** Used for ONE_TIME schedule */
    private LocalDateTime executionDatetime;

    /** Time of day for RECURRING schedules */
    private LocalTime executionTime;

    /** Recurring window start date */
    private LocalDate startDate;

    /** Recurring window end date (null = no end) */
    private LocalDate endDate;

    /** Day of month for MONTHLY recurrence (1-31) */
    private Integer dayOfMonth;

    /** IANA timezone string, e.g. "Asia/Kolkata" */
    @Column(length = 100)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActivityStatus status = ActivityStatus.ACTIVE;

    private LocalDateTime nextExecutionAt;

    private LocalDateTime lastExecutionAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
