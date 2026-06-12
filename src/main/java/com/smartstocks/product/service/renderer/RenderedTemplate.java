package com.smartstocks.product.service.renderer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenderedTemplate {

    private final String renderedSubject;
    private final String renderedBody;
}
