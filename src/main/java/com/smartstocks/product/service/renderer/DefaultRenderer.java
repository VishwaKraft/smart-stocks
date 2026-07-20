package com.smartstocks.product.service.renderer;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default renderer — resolves {{variableName}} placeholders using simple regex.
 * Also supports basic {{#each listName}} ... {{/each}} loops.
 */
@Component
public class DefaultRenderer implements ITemplateRenderer {

    private static final Pattern EACH_BLOCK = Pattern.compile("\\{\\{#each\\s+(\\w+)\\s*}}(.*?)\\{\\{/each}}", Pattern.DOTALL);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    @Override
    public RenderedTemplate render(String subject, String body, Map<String, Object> variables) {
        return new RenderedTemplate(
                resolve(subject, variables),
                resolve(body, variables)
        );
    }

    private String resolve(String template, Map<String, Object> variables) {
        if (template == null) return "";
        
        // First process {{#each}} blocks
        StringBuffer result = new StringBuffer();
        Matcher eachMatcher = EACH_BLOCK.matcher(template);
        while (eachMatcher.find()) {
            String listKey = eachMatcher.group(1);
            String blockContent = eachMatcher.group(2);
            Object listObj = variables.get(listKey);
            
            StringBuilder resolvedBlock = new StringBuilder();
            if (listObj instanceof Iterable) {
                for (Object item : (Iterable<?>) listObj) {
                    Map<String, Object> localVars = new HashMap<>(variables);
                    if (item instanceof Map) {
                        Map<?, ?> mapItem = (Map<?, ?>) item;
                        for (Map.Entry<?, ?> entry : mapItem.entrySet()) {
                            localVars.put("this." + entry.getKey(), entry.getValue());
                            localVars.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    } else {
                        localVars.put("this", item);
                    }
                    resolvedBlock.append(resolvePlaceholders(blockContent, localVars));
                }
            }
            eachMatcher.appendReplacement(result, Matcher.quoteReplacement(resolvedBlock.toString()));
        }
        eachMatcher.appendTail(result);
        
        // Then process simple placeholders in the remaining text
        return resolvePlaceholders(result.toString(), variables);
    }
    
    private String resolvePlaceholders(String template, Map<String, Object> variables) {
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
