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
@Table(name = "link_click_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LinkClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String shortId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    private Long userId;

    @Column(length = 255)
    private String campaign;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent = "";

    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
