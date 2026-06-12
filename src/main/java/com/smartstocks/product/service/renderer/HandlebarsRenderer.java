package com.smartstocks.product.service.renderer;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Renderer backed by the Handlebars.java library.
 * Supports {{variable}}, {{#if}}, {{#each}}, partials, helpers, etc.
 */
@Slf4j
@Component
public class HandlebarsRenderer implements ITemplateRenderer {

    private final Handlebars handlebars = new Handlebars();

    @Override
    public RenderedTemplate render(String subject, String body, Map<String, Object> variables) {
        return new RenderedTemplate(
                compile(subject, variables, "subject"),
                compile(body, variables, "body")
        );
    }

    private String compile(String source, Map<String, Object> variables, String contextName) {
        if (source == null) return "";
        try {
            Template template = handlebars.compileInline(source);
            return template.apply(variables);
        } catch (IOException e) {
            log.error("Handlebars rendering failed for [{}]: {}", contextName, e.getMessage());
            return source;
        }
    }
}
