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
@Table(name = "event_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventLog {

    @Id
    @GeneratedValue
    private Long eventId;

    @Column(nullable = false)
    private String eventType;

    private Long userId;

    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> eventInfo;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent = "";

    private LocalDateTime timestamp;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
