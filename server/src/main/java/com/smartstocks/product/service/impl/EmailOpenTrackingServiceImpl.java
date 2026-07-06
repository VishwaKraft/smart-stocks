package com.smartstocks.product.service.impl;

import com.smartstocks.product.models.EmailOpenEvent;
import com.smartstocks.product.models.User;
import com.smartstocks.product.repository.EmailOpenEventRepository;
import com.smartstocks.product.service.IEmailOpenTrackingService;
import com.smartstocks.product.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailOpenTrackingServiceImpl implements IEmailOpenTrackingService {

    @Autowired
    private EmailOpenEventRepository emailOpenEventRepository;

    @Autowired
    private IUserService userService;

    @Override
    @Transactional
    public void trackOpen(
            Long userId,
            Long campaignId,
            Long activityId,
            String campaignCode,
            String emailId,
            Map<String, Object> metadata,
            String ipAddress,
            String userAgent,
            Map<String, String> requestHeaders,
            boolean isProxyOpen,
            Principal principal) {

        EmailOpenEvent event = new EmailOpenEvent();
        event.setUserId(resolveUserId(userId, principal));
        event.setCampaignId(campaignId);
        event.setActivityId(activityId);
        event.setCampaign(campaignCode);
        event.setEmailId(emailId);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent != null ? userAgent : "");
        event.setProxyOpen(isProxyOpen);
        event.setMetadata(mergeMetadata(metadata, requestHeaders, isProxyOpen));
        event.setOpenedAt(LocalDateTime.now());

        emailOpenEventRepository.save(event);
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

    private Map<String, Object> mergeMetadata(Map<String, Object> metadata, Map<String, String> requestHeaders, boolean isProxyOpen) {
        Map<String, Object> merged = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            merged.put("headers", requestHeaders);
        }
        if (isProxyOpen) {
            merged.put("proxy_open", true);
            merged.put("proxy_note", "IP belongs to caching proxy, not the real reader");
        }
        return merged;
    }
}
