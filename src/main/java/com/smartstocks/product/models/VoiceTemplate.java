package com.smartstocks.product.models;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores reusable voice (TTS) templates for Infobip voice campaigns.
 * Each template holds the message text, language, and voice configuration
 * that is sent to the Infobip /tts/3/advanced API.
 */
@Entity
@Table(name = "voice_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable template name for identification in the UI. */
    @Column(nullable = false, length = 255)
    private String name;

    /** The TTS message text to be spoken. Supports basic variable placeholders like {{firstName}}. */
    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    /**
     * BCP-47 / Infobip language code (e.g. "en", "hi", "en-US").
     * Defaults to "en".
     */
    @Column(length = 20)
    private String language = "en";

    /**
     * Infobip voice name (e.g. "Joanna", "Matthew", "Aditi").
     * Defaults to "Joanna".
     */
    @Column(name = "voice_name", length = 50)
    private String voiceName = "Joanna";

    /**
     * Voice gender — "female" or "male".
     * Defaults to "female".
     */
    @Column(name = "voice_gender", length = 10)
    private String voiceGender = "female";

    /** Optional: tie this template to a specific campaign for scoped listing. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
