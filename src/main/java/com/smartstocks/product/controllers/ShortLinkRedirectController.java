package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.EventLogRequestDto;
import com.smartstocks.product.models.ShortLink;
import com.smartstocks.product.service.IEventLogService;
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

    private static final String EVENT_TYPE = "short_link_click";

    @Autowired
    private IShortLinkService shortLinkService;

    @Autowired
    private IEventLogService eventLogService;

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

        logClickEvent(shortId, link, userId, campaign, httpRequest, principal);
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

    private void logClickEvent(
            String shortId,
            ShortLink link,
            Long userId,
            String campaign,
            HttpServletRequest httpRequest,
            Principal principal) {
        EventLogRequestDto request = new EventLogRequestDto();
        request.setEventType(EVENT_TYPE);
        request.setUserId(userId);
        request.setEventInfo(buildEventInfo(shortId, link, campaign, httpRequest));

        eventLogService.logEvent(
                request,
                HttpRequestUtils.resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                HttpRequestUtils.extractHeaders(httpRequest),
                principal
        );
    }

    private Map<String, Object> buildEventInfo(
            String shortId,
            ShortLink link,
            String campaign,
            HttpServletRequest request) {
        Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("short_id", shortId);
        eventInfo.put("original_url", link.getOriginalUrl());
        eventInfo.put("source", "url_shortener");

        if (campaign != null && !campaign.isBlank()) {
            eventInfo.put("campaign", campaign);
        }

        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0 && !eventInfo.containsKey(key)) {
                eventInfo.put(key, values[0]);
            }
        });

        return eventInfo;
    }
}
