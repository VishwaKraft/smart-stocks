package com.smartstocks.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled annotation processing.
 * Required for CampaignScheduler to fire.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
