package com.smartstocks.product.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * Known email-client caching proxies / bots that pre-fetch pixel images.
     * When matched, the recorded IP belongs to the proxy, not the real reader.
     */
    private static final List<String> KNOWN_PROXY_UA_FRAGMENTS = List.of(
            "googleimageproxy",   // Gmail image proxy (accounts for most false IPs)
            "yahoo! slurp",       // Yahoo Mail image prefetch
            "yahoo slurp",
            "applebot",           // Apple Mail Privacy Protection
            "icloud",             // Apple iCloud Mail proxy
            "outlook-link-rewrite", // Microsoft Safe Links
            "microsoft url preview",
            "msnbot",
            "bingpreview",
            "duckduckbot",
            "facebookexternalhit", // LinkedIn / Facebook link-preview crawlers
            "linkedinbot",
            "twitterbot",
            "slackbot"
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

    /**
     * Resolves the real client IP by checking several well-known forwarding headers
     * in priority order before falling back to {@code getRemoteAddr()}.
     *
     * <p>Priority:
     * <ol>
     *   <li>{@code CF-Connecting-IP} – Cloudflare's verified original-client header</li>
     *   <li>{@code True-Client-IP} – Akamai / Cloudflare enterprise header</li>
     *   <li>{@code X-Real-IP} – Common nginx reverse-proxy header</li>
     *   <li>First value in {@code X-Forwarded-For}</li>
     *   <li>{@code getRemoteAddr()} as last resort</li>
     * </ol>
     */
    public static String resolveClientIp(HttpServletRequest request) {
        // Cloudflare: the most trustworthy single-value header
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (isValidIp(cfIp)) {
            return cfIp.trim();
        }

        // Akamai / Cloudflare enterprise
        String trueClientIp = request.getHeader("True-Client-IP");
        if (isValidIp(trueClientIp)) {
            return trueClientIp.trim();
        }

        // Common nginx header
        String realIp = request.getHeader("X-Real-IP");
        if (isValidIp(realIp)) {
            return realIp.trim();
        }

        // Standard proxy chain – take the left-most (originating) address
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Returns {@code true} when the User-Agent belongs to a known email-client
     * caching proxy (e.g. Gmail Image Proxy, Apple Mail Privacy Protection).
     *
     * <p>When this returns {@code true} the IP address logged with the open event
     * will be the proxy's IP, <em>not</em> the actual reader's IP. Callers should
     * flag the event accordingly rather than treating it as a genuine human open.
     *
     * @param userAgent the {@code User-Agent} header value; may be {@code null}
     */
    public static boolean isProxyOrBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        for (String fragment : KNOWN_PROXY_UA_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidIp(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("unknown");
    }

    private static boolean isRedactedHeader(String name) {
        return REDACTED_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }
}
