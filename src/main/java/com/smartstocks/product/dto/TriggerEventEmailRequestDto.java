package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEventEmailRequestDto {

    /**
     * List of recipient email addresses to send the email to.
     * At least one recipient is required.
     */
    @NotEmpty(message = "At least one recipient email is required")
    private List<String> recipients;

    /**
     * Optional Handlebars/Mustache variable map for template rendering.
     * Example: {"name": "Alice", "plan": "Pro"}
     */
    private Map<String, Object> variables;
}
