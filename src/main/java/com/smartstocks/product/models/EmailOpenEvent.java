package com.smartstocks.product.models;

import com.smartstocks.product.converters.MapToJsonConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "email_open_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailOpenEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long campaignId;

    /** The activity that sent this email (for per-activity open-rate analytics) */
    private Long activityId;

    @Column(length = 255)
    private String campaign;

    @Column(length = 255)
    private String emailId;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent = "";

    /** General metadata map (campaign info, headers, source, etc.) */
    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    /**
     * Extra arbitrary data passed through the pixel URL (e.g. from the segment's
     * per-recipient data). Stored as JSON for extensibility.
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private Map<String, Object> extraData;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

