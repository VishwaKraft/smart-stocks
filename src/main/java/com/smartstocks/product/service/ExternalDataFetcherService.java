package com.smartstocks.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fetches external JSON data from a URL and normalises the result to a
 * {@code Map<String, Object>} regardless of whether the remote API returns
 * a JSON object ({@code {...}}) or a JSON array ({@code [{...}, ...]}).
 *
 * <p>When the response is an array the <em>first</em> element is used, which
 * covers APIs like ZenQuotes that always wrap a single object in a list.</p>
 *
 * <p>Returns {@code null} on any network or parse error so callers can safely
 * fall back to an empty variable set without aborting the campaign send.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalDataFetcherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Fetches JSON from {@code url} and returns it as a flat map.
     *
     * @param url the remote data-source URL
     * @return a non-null map on success, {@code null} on any error
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            log.info("[ExternalDataFetcher] Hitting external API: {}", url);
            String raw = restTemplate.getForObject(url, String.class);
            if (raw == null || raw.isBlank()) {
                log.warn("[ExternalDataFetcher] Empty response from URL: {}", url);
                return null;
            }

            String trimmed = raw.stripLeading();
            Map<String, Object> result;

            if (trimmed.startsWith("[")) {
                // API returned a JSON array — use the first element
                List<Map<String, Object>> list = objectMapper.readValue(
                        raw,
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, Map.class));
                if (list == null || list.isEmpty()) {
                    log.warn("[ExternalDataFetcher] Empty array response from URL: {}", url);
                    return null;
                }
                log.info("[ExternalDataFetcher] Array response ({} element(s)) — using element[0] from URL: {}", list.size(), url);
                result = list.get(0);
            } else {
                // API returned a plain JSON object
                result = objectMapper.readValue(
                        raw,
                        objectMapper.getTypeFactory()
                                .constructMapType(Map.class, String.class, Object.class));
            }

            log.info("[ExternalDataFetcher] Data fetched successfully ({} key(s)) from URL: {} — keys: {}",
                    result == null ? 0 : result.size(), url,
                    result == null ? "[]" : result.keySet());
            log.debug("[ExternalDataFetcher] Full external data payload: {}", result);
            return result;

        } catch (Exception e) {
            log.error("[ExternalDataFetcher] Failed to fetch or parse data from URL: {}", url, e);
            return null;
        }
    }
}
