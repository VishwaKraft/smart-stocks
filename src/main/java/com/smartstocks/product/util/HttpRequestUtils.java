package com.smartstocks.product.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HttpRequestUtils {

    private static final Set<String> REDACTED_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "proxy-authorization"
    );

    private HttpRequestUtils() {
    }

    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return headers;
        }

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            if (isRedactedHeader(name)) {
                value = "[REDACTED]";
            }
            headers.put(name, value);
        }
        return headers;
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isRedactedHeader(String name) {
        return REDACTED_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }
}
