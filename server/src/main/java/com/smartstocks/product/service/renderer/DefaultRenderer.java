package com.smartstocks.product.service.renderer;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default renderer — resolves {{variableName}} placeholders using simple regex.
 * Suitable for lightweight templates that don't need a full template engine.
 */
@Component
public class DefaultRenderer implements ITemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    @Override
    public RenderedTemplate render(String subject, String body, Map<String, Object> variables) {
        return new RenderedTemplate(
                resolve(subject, variables),
                resolve(body, variables)
        );
    }

    private String resolve(String template, Map<String, Object> variables) {
        if (template == null) return "";
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
