package com.smartstocks.product.controllers;

import com.smartstocks.product.models.ShortLink;
import com.smartstocks.product.service.ILinkClickTrackingService;
import com.smartstocks.product.service.IShortLinkService;
import com.smartstocks.product.util.HttpRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/s")
public class ShortLinkRedirectController {

    @Autowired
    private IShortLinkService shortLinkService;

    @Autowired
    private ILinkClickTrackingService linkClickTrackingService;

    @GetMapping("/{shortId}")
    public String redirectToOriginal(
            @PathVariable String shortId,
            @RequestParam(value = "user_id", required = false) Long userId,
            @RequestParam(value = "campaign", required = false) String campaign,
            HttpServletRequest httpRequest,
            Principal principal) {

        ShortLink link = shortLinkService.getLinkByShortId(shortId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        String originalUrl = link.getOriginalUrl();

        linkClickTrackingService.trackClick(
                shortId,
                originalUrl,
                userId,
                campaign,
                buildMetadata(shortId, link, campaign, httpRequest),
                HttpRequestUtils.resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                HttpRequestUtils.extractHeaders(httpRequest),
                principal
        );
        shortLinkService.incrementClickCountAsync(shortId);

        return "redirect:" + originalUrl;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleRedirectError(ResponseStatusException ex) {
        if (ex.getStatus() == HttpStatus.NOT_FOUND) {
            return "short-links/notFound";
        }
        throw ex;
    }

    private Map<String, Object> buildMetadata(
            String shortId,
            ShortLink link,
            String campaign,
            HttpServletRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("short_id", shortId);
        metadata.put("original_url", link.getOriginalUrl());
        metadata.put("source", "url_shortener");

        if (campaign != null && !campaign.isBlank()) {
            metadata.put("campaign", campaign);
        }

        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0 && !metadata.containsKey(key)) {
                metadata.put(key, values[0]);
            }
        });

        return metadata;
    }
}
