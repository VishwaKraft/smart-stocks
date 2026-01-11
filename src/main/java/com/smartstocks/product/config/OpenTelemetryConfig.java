package com.smartstocks.product.config;

import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Configuration
 * 
 * The OpenTelemetry Spring Boot Starter will automatically configure
 * OpenTelemetry based on the properties in application.yml:
 * - otel.exporter.otlp.endpoint
 * - otel.resource.attributes.service.name
 * - otel.resource.attributes.service.version
 * 
 * No manual configuration needed - the starter handles everything.
 */
@Configuration
public class OpenTelemetryConfig {
    // OpenTelemetry Spring Boot Starter handles autoconfiguration
    // Configuration is done via application.yml properties
}

