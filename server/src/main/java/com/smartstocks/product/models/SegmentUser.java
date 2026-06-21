package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "segment_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private Segment segment;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "email_id", nullable = false)
    private String emailId;

    @Column(name = "phone_number")
    private String phoneNumber;
}
