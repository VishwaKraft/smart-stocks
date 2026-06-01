package com.smartstocks.product.service.impl;

import com.smartstocks.product.models.LinkClickEvent;
import com.smartstocks.product.models.User;
import com.smartstocks.product.repository.LinkClickEventRepository;
import com.smartstocks.product.service.ILinkClickTrackingService;
import com.smartstocks.product.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LinkClickTrackingServiceImpl implements ILinkClickTrackingService {

    @Autowired
    private LinkClickEventRepository linkClickEventRepository;

    @Autowired
    private IUserService userService;

    @Override
    @Transactional
    public void trackClick(
            String shortId,
            String originalUrl,
            Long userId,
            String campaign,
            Map<String, Object> metadata,
            String ipAddress,
            String userAgent,
            Map<String, String> requestHeaders,
            Principal principal) {
        LinkClickEvent event = new LinkClickEvent();
        event.setShortId(shortId);
        event.setOriginalUrl(originalUrl);
        event.setUserId(resolveUserId(userId, principal));
        event.setCampaign(campaign);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent != null ? userAgent : "");
        event.setMetadata(mergeMetadata(metadata, requestHeaders));
        event.setClickedAt(LocalDateTime.now());

        linkClickEventRepository.save(event);
    }

    private Long resolveUserId(Long requestedUserId, Principal principal) {
        if (requestedUserId != null) {
            return requestedUserId;
        }
        if (principal != null) {
            User user = userService.getUserByEmail(principal.getName());
            return user.getUserId();
        }
        return null;
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> metadata, Map<String, String> requestHeaders) {
        Map<String, Object> merged = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            merged.put("headers", requestHeaders);
        }
        return merged;
    }
}
