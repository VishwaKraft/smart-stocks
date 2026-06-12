package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "campaign_activity_weekdays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignActivityWeekday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private CampaignActivity activity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Weekday weekday;
}
