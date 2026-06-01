package com.smartstocks.product.service;

import java.security.Principal;
import java.util.Map;

public interface ILinkClickTrackingService {

    void trackClick(
            String shortId,
            String originalUrl,
            Long userId,
            String campaign,
            Map<String, Object> metadata,
            String ipAddress,
            String userAgent,
            Map<String, String> requestHeaders,
            Principal principal);
}
