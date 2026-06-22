package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "segment_type", nullable = false)
    private String segmentType; // CSV or SQL

    @Column(name = "s3_path")
    private String s3Path;

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
