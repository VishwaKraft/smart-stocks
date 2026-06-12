package com.smartstocks.product.service.renderer;

import com.smartstocks.product.models.RendererType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that resolves the correct ITemplateRenderer based on RendererType.
 * Adding a new renderer: implement ITemplateRenderer, register it here — no other changes needed.
 */
@Component
public class TemplateRendererFactory {

    private final Map<RendererType, ITemplateRenderer> renderers = new EnumMap<>(RendererType.class);

    public TemplateRendererFactory(
            DefaultRenderer defaultRenderer,
            HandlebarsRenderer handlebarsRenderer,
            MustacheRenderer mustacheRenderer,
            VelocityRenderer velocityRenderer) {

        renderers.put(RendererType.DEFAULT, defaultRenderer);
        renderers.put(RendererType.HANDLEBARS, handlebarsRenderer);
        renderers.put(RendererType.MUSTACHE, mustacheRenderer);
        renderers.put(RendererType.VELOCITY, velocityRenderer);
    }

    /**
     * Returns the renderer for the given type.
     *
     * @throws IllegalArgumentException if no renderer is registered for the type
     */
    public ITemplateRenderer get(RendererType type) {
        ITemplateRenderer renderer = renderers.get(type);
        if (renderer == null) {
            throw new IllegalArgumentException("No renderer registered for type: " + type);
        }
        return renderer;
    }
}
