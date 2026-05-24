package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.EventLogRequestDto;
import com.smartstocks.product.service.IEventLogService;
import com.smartstocks.product.util.HttpRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tracking")
@CrossOrigin(origins = "*")
public class EmailTrackingController {

    private static final String PIXEL_IMAGE_PATH = "static/tracking/email_stocks.png";
    private static final String EVENT_TYPE = "email_open";

    @Autowired
    private IEventLogService eventLogService;

    @GetMapping(value = "/email_stocks.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> trackEmailOpen(
            @RequestParam(value = "user_id", required = false) Long userId,
            @RequestParam(value = "campaign", required = false) String campaign,
            @RequestParam(value = "email_id", required = false) String emailId,
            HttpServletRequest httpRequest,
            Principal principal) throws IOException {

        EventLogRequestDto request = new EventLogRequestDto();
        request.setEventType(EVENT_TYPE);
        request.setUserId(userId);
        request.setEventInfo(buildEventInfo(httpRequest, campaign, emailId));

        eventLogService.logEvent(
                request,
                HttpRequestUtils.resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                HttpRequestUtils.extractHeaders(httpRequest),
                principal
        );

        ClassPathResource image = new ClassPathResource(PIXEL_IMAGE_PATH);
        byte[] imageBytes = StreamUtils.copyToByteArray(image.getInputStream());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl(CacheControl.noStore().mustRevalidate());

        return ResponseEntity.ok().headers(headers).body(imageBytes);
    }

    private Map<String, Object> buildEventInfo(HttpServletRequest request, String campaign, String emailId) {
        Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("image", "email_stocks.png");
        eventInfo.put("source", "email_pixel");

        if (campaign != null && !campaign.isBlank()) {
            eventInfo.put("campaign", campaign);
        }
        if (emailId != null && !emailId.isBlank()) {
            eventInfo.put("email_id", emailId);
        }

        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0 && !eventInfo.containsKey(key)) {
                eventInfo.put(key, values[0]);
            }
        });

        return eventInfo;
    }
}
