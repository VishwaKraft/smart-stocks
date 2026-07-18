package com.smartstocks.product.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String campaignCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CampaignType campaignType = CampaignType.EMAIL;

    @Column(length = 32)
    private String whatsappSenderNumber;

    /** Email delivery provider to use when executing activities for this campaign */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmailProviderType emailProviderType;

    @Column(length = 2048)
    private String googleAccessToken;

    @Column(length = 2048)
    private String googleRefreshToken;

    /** Permanent Meta/WhatsApp access token stored after "Sign in with Meta" */
    @Column(length = 2048)
    private String metaAccessToken;

    /** WhatsApp Phone Number ID from Meta App Dashboard (e.g. 1095078330366468) */
    @Column(length = 64)
    private String metaPhoneNumberId;

    /** Phone number used as the "from" in Infobip voice (TTS) campaigns (e.g. "38515507799"). */
    @Column(name = "infobip_sender_number", length = 32)
    private String infobipSenderNumber;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
