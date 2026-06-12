package com.smartstocks.product.service.renderer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * Renderer backed by mustache.java.
 * Supports {{variable}}, {{#section}}, {{^inverted}}, partials, etc.
 */
@Slf4j
@Component
public class MustacheRenderer implements ITemplateRenderer {

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

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
            Mustache mustache = mustacheFactory.compile(new StringReader(source), contextName);
            StringWriter writer = new StringWriter();
            mustache.execute(writer, variables).flush();
            return writer.toString();
        } catch (Exception e) {
            log.error("Mustache rendering failed for [{}]: {}", contextName, e.getMessage());
            return source;
        }
    }
}
