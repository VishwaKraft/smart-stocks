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

    @Column(length = 255)
    private String campaign;

    @Column(length = 255)
    private String emailId;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent = "";

    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
