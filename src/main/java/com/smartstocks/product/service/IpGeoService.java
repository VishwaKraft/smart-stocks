package com.smartstocks.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves geographic and ISP metadata for an IPv4/IPv6 address using the
 * free ip-api.com endpoint: {@code http://ip-api.com/json/<ip>}.
 *
 * <p>Failures (network errors, unknown IPs, private/loopback ranges) are
 * handled silently — the caller receives an empty map so event-log saving
 * is never blocked by a geo-lookup failure.
 *
 * <p><b>Rate limit:</b> ip-api.com allows 45 requests/minute on the free tier.
 * Production deployments with higher traffic should consider upgrading to the
 * paid plan or adding a local cache.
 */
@Service
public class IpGeoService {

    private static final Logger log = LoggerFactory.getLogger(IpGeoService.class);

    private static final String IP_API_URL = "http://ip-api.com/json/%s?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,query";

    /**
     * IPs that belong to loopback or RFC-1918 private ranges – ip-api returns
     * "fail" for these, so we skip the network call entirely.
     */
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "127.", "10.", "192.168.", "::1", "0:0:0:0:0:0:0:1"
    );

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Looks up geo/ISP data for the given IP address.
     *
     * @param ipAddress the raw IP address string (may be {@code null} or blank)
     * @return a map of geo fields to include in {@code event_info}; never {@code null}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> lookup(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return Collections.emptyMap();
        }

        String ip = ipAddress.trim();

        // Skip private / loopback addresses – no point hitting the API
        if (isPrivateOrLoopback(ip)) {
            log.debug("Skipping ip-api lookup for private/loopback address: {}", ip);
            return Collections.emptyMap();
        }

        try {
            String url = String.format(IP_API_URL, ip);
            String json = restTemplate.getForObject(url, String.class);
            if (json == null || json.isBlank()) {
                return Collections.emptyMap();
            }

            Map<String, Object> raw = objectMapper.readValue(json, Map.class);

            // ip-api returns {"status":"fail"} for unknown or reserved IPs
            if (!"success".equals(raw.get("status"))) {
                log.debug("ip-api lookup failed for {}: {}", ip, raw.get("message"));
                return Collections.emptyMap();
            }

            return buildGeoMap(raw);

        } catch (Exception e) {
            log.warn("ip-api geo-lookup failed for IP '{}': {}", ip, e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildGeoMap(Map<String, Object> raw) {
        Map<String, Object> geo = new LinkedHashMap<>();
        putIfPresent(geo, "ip",          raw.get("query"));
        putIfPresent(geo, "country",     raw.get("country"));
        putIfPresent(geo, "country_code",raw.get("countryCode"));
        putIfPresent(geo, "region",      raw.get("regionName"));
        putIfPresent(geo, "region_code", raw.get("region"));
        putIfPresent(geo, "city",        raw.get("city"));
        putIfPresent(geo, "zip",         raw.get("zip"));
        putIfPresent(geo, "lat",         raw.get("lat"));
        putIfPresent(geo, "lon",         raw.get("lon"));
        putIfPresent(geo, "timezone",    raw.get("timezone"));
        putIfPresent(geo, "isp",         raw.get("isp"));
        putIfPresent(geo, "org",         raw.get("org"));
        putIfPresent(geo, "as",          raw.get("as"));
        return geo;
    }

    private void putIfPresent(Map<String, Object> dest, String key, Object value) {
        if (value != null) {
            dest.put(key, value);
        }
    }

    private boolean isPrivateOrLoopback(String ip) {
        for (String prefix : SKIP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        // 172.16.0.0/12 range: 172.16.x.x – 172.31.x.x
        if (ip.startsWith("172.")) {
            try {
                int secondOctet = Integer.parseInt(ip.split("\\.")[1]);
                if (secondOctet >= 16 && secondOctet <= 31) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // not a standard IPv4, let the API decide
            }
        }
        return false;
    }
}
