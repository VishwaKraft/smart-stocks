package com.smartstocks.product.service.renderer;

import java.util.Map;

/**
 * Strategy interface for template rendering engines.
 * Each implementation processes {{variable}} placeholders using its own syntax.
 */
public interface ITemplateRenderer {

    /**
     * Render the given subject and body by substituting variables.
     *
     * @param subject   raw subject string (may contain variable placeholders)
     * @param body      raw HTML body string (may contain variable placeholders)
     * @param variables key-value map of template variables
     * @return RenderedTemplate with fully resolved subject and body
     */
    RenderedTemplate render(String subject, String body, Map<String, Object> variables);
}
