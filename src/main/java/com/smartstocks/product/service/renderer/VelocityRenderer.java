package com.smartstocks.product.service.renderer;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;

/**
 * Renderer backed by Apache Velocity.
 * Supports $variable, #if(), #foreach(), #set(), macros, etc.
 */
@Slf4j
@Component
public class VelocityRenderer implements ITemplateRenderer {

    private final VelocityEngine velocityEngine;

    public VelocityRenderer() {
        this.velocityEngine = new VelocityEngine();
        this.velocityEngine.setProperty("resource.loader", "string");
        this.velocityEngine.setProperty("string.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
        this.velocityEngine.init();
    }

    @Override
    public RenderedTemplate render(String subject, String body, Map<String, Object> variables) {
        return new RenderedTemplate(
                evaluate(subject, variables, "subject"),
                evaluate(body, variables, "body")
        );
    }

    private String evaluate(String source, Map<String, Object> variables, String logTag) {
        if (source == null) return "";
        try {
            VelocityContext context = new VelocityContext();
            variables.forEach(context::put);
            StringWriter writer = new StringWriter();
            velocityEngine.evaluate(context, writer, logTag, source);
            return writer.toString();
        } catch (Exception e) {
            log.error("Velocity rendering failed for [{}]: {}", logTag, e.getMessage());
            return source;
        }
    }
}
