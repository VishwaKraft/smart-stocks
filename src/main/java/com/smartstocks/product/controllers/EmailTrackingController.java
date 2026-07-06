package com.smartstocks.product.controllers;

import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.IEmailOpenTrackingService;
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
import java.util.Optional;

@RestController
@RequestMapping("/tracking")
@CrossOrigin(origins = "*")
public class EmailTrackingController {

    private static final String PIXEL_IMAGE_PATH = "static/tracking/email_stocks.png";

    @Autowired
    private IEmailOpenTrackingService emailOpenTrackingService;

    @Autowired
    private ICampaignService campaignService;

    /**
     * Pixel-tracking endpoint.
     *
     * <p>Each recipient receives a URL that contains a unique {@code nonce} (UUID) so that
     * caching proxies (Google Image Proxy, Apple Mail Privacy Protection, etc.) cannot serve
     * a cached copy — every URL is distinct, forcing a fresh network request that reaches
     * this server with the real client IP.
     *
     * <p>The response also carries the strongest possible cache-busting headers:
     * {@code Cache-Control: no-store, must-revalidate}, {@code Pragma: no-cache}, and
     * {@code Expires: 0}. While proxies can ignore these, the combination of unique URL +
     * no-cache headers gives the best chance of recording the reader's actual IP.
     *
     * <p>When the User-Agent is recognised as a known caching bot (Gmail proxy, Yahoo Slurp,
     * Apple iCloud, etc.) the event is saved with {@code proxyOpen = true} so analytics
     * queries can exclude or separately count proxy-inflated opens.
     */
    @GetMapping(value = "/email_stocks.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> trackEmailOpen(
            @RequestParam(value = "user_id", required = false) Long userId,
            @RequestParam(value = "campaign", required = false) String campaign,
            @RequestParam(value = "campaign_id", required = false) Long campaignId,
            @RequestParam(value = "email_id", required = false) String emailId,
            @RequestParam(value = "activity_id", required = false) Long activityId,
            // Unique nonce appended per-recipient to defeat caching proxies
            @RequestParam(value = "nonce", required = false) String nonce,
            HttpServletRequest httpRequest,
            Principal principal) throws IOException {

        String userAgent = httpRequest.getHeader("User-Agent");
        boolean isProxyOpen = HttpRequestUtils.isProxyOrBot(userAgent);

        Optional<Campaign> resolvedCampaign = resolveCampaign(campaignId, campaign);

        emailOpenTrackingService.trackOpen(
                userId,
                resolvedCampaign.map(Campaign::getId).orElse(null),
                activityId,
                resolvedCampaign.map(Campaign::getCampaignCode).orElse(campaign),
                emailId,
                buildMetadata(httpRequest, resolvedCampaign, campaign, emailId, activityId, nonce, isProxyOpen),
                HttpRequestUtils.resolveClientIp(httpRequest),
                userAgent,
                HttpRequestUtils.extractHeaders(httpRequest),
                isProxyOpen,
                principal
        );

        ClassPathResource image = new ClassPathResource(PIXEL_IMAGE_PATH);
        byte[] imageBytes = StreamUtils.copyToByteArray(image.getInputStream());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        // Aggressive cache-busting: instruct every intermediary NOT to cache this response.
        // Combined with a unique per-recipient nonce in the URL, this maximises the chance
        // that the request reaches our server directly rather than being served from a
        // Google / Yahoo / Apple caching proxy.
        headers.setCacheControl(CacheControl.noStore().mustRevalidate());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok().headers(headers).body(imageBytes);
    }

    private Optional<Campaign> resolveCampaign(Long campaignId, String campaignCode) {
        if (campaignId != null) {
            Optional<Campaign> byId = campaignService.findById(campaignId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (campaignCode != null && !campaignCode.isBlank()) {
            return campaignService.findByCampaignCode(campaignCode);
        }
        return Optional.empty();
    }

    private Map<String, Object> buildMetadata(
            HttpServletRequest request,
            Optional<Campaign> resolvedCampaign,
            String campaignParam,
            String emailId,
            Long activityId,
            String nonce,
            boolean isProxyOpen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("image", "email_stocks.png");
        metadata.put("source", "email_pixel");

        resolvedCampaign.ifPresent(c -> {
            metadata.put("campaign_id", c.getId());
            metadata.put("campaign", c.getCampaignCode());
            metadata.put("campaign_name", c.getName());
        });

        if (!resolvedCampaign.isPresent() && campaignParam != null && !campaignParam.isBlank()) {
            metadata.put("campaign", campaignParam);
        }
        if (emailId != null && !emailId.isBlank()) {
            metadata.put("email_id", emailId);
        }
        if (activityId != null) {
            metadata.put("activity_id", activityId);
        }
        if (nonce != null && !nonce.isBlank()) {
            // Store the nonce so we can correlate this open with the specific sent message
            metadata.put("nonce", nonce);
        }
        if (isProxyOpen) {
            metadata.put("proxy_open", true);
        }

        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0 && !metadata.containsKey(key)) {
                metadata.put(key, values[0]);
            }
        });

        return metadata;
    }
}
