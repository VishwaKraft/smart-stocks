package com.smartstocks.product.service;

import java.security.Principal;
import java.util.Map;

public interface IEmailOpenTrackingService {

    void trackOpen(
            Long userId,
            Long campaignId,
            String campaignCode,
            String emailId,
            Map<String, Object> metadata,
            String ipAddress,
            String userAgent,
            Map<String, String> requestHeaders,
            Principal principal);
}
