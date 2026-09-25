package com.smartstocks.product.controllers;

import com.smartstocks.product.models.Campaign;
import com.smartstocks.product.models.CampaignActivity;
import com.smartstocks.product.models.CampaignUnsubscribe;
import com.smartstocks.product.repository.CampaignActivityRepository;
import com.smartstocks.product.repository.CampaignUnsubscribeRepository;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.util.HttpRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Public endpoint that handles one-click email unsubscribes.
 *
 * <p>Flow:
 * <ol>
 *   <li>Recipient opens an email and clicks the unsubscribe link at the bottom.</li>
 *   <li>Browser hits {@code GET /tracking/unsubscribe?email_id=...&campaign=...&activity_id=...}.</li>
 *   <li>The server records a {@link CampaignUnsubscribe} entry (idempotent — duplicate clicks are silently ignored).</li>
 *   <li>A styled HTML confirmation page is returned directly so the user sees immediate feedback.</li>
 * </ol>
 *
 * <p>On the next activity execution for any activity in the same campaign, the
 * {@link com.smartstocks.product.scheduler.CampaignScheduler} and
 * {@link com.smartstocks.product.service.impl.CampaignActivityServiceImpl#generateActivityData}
 * both filter out unsubscribed email addresses from the recipient list.
 */
@RestController
@RequestMapping("/tracking")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class UnsubscribeController {

    private final CampaignUnsubscribeRepository unsubscribeRepository;
    private final ICampaignService campaignService;
    private final CampaignActivityRepository activityRepository;

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleUnsubscribe(
            @RequestParam(value = "email_id", required = false) String emailId,
            @RequestParam(value = "campaign", required = false) String campaignCode,
            @RequestParam(value = "campaign_id", required = false) Long campaignId,
            @RequestParam(value = "activity_id", required = false) Long activityId,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest httpRequest) {

        if (emailId == null || emailId.isBlank()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildPage("Invalid Request",
                            "No email address was provided in the unsubscribe link.",
                            false));
        }

        // ------------------------------------------------------------------
        // 1. Verify HMAC-MD5 token — reject forged / tampered links
        // ------------------------------------------------------------------
        String normalisedEmail  = emailId.trim().toLowerCase();
        String normalisedCampaign = campaignCode != null ? campaignCode : "";
        String expectedToken = campaignService.computeUnsubscribeToken(normalisedEmail, normalisedCampaign);

        if (token == null || token.isBlank() || !expectedToken.equalsIgnoreCase(token.trim())) {
            log.warn("[Unsubscribe] Invalid or missing token for email={} campaign={}. Request rejected.",
                    emailId, campaignCode);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildPage("Invalid Unsubscribe Link",
                            "This unsubscribe link is invalid or has been tampered with. " +
                            "Please use the original link from your email.",
                            false));
        }

        // Resolve campaign entity
        Optional<Campaign> campaignOpt = resolveCampaign(campaignId, campaignCode);
        if (campaignOpt.isEmpty()) {
            log.warn("[Unsubscribe] Campaign not found (id={}, code={}). Ignoring request for email={}.",
                    campaignId, campaignCode, emailId);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildPage("Already Unsubscribed",
                            "You have been successfully unsubscribed.",
                            true));
        }

        Campaign campaign = campaignOpt.get();

        // Idempotent — do not create a duplicate record
        boolean alreadyUnsubscribed = unsubscribeRepository
                .existsByCampaignIdAndEmailId(campaign.getId(), normalisedEmail);

        if (!alreadyUnsubscribed) {
            CampaignActivity activity = null;
            if (activityId != null) {
                activity = activityRepository.findById(activityId).orElse(null);
            }

            CampaignUnsubscribe record = CampaignUnsubscribe.builder()
                    .campaign(campaign)
                    .emailId(normalisedEmail)
                    .activity(activity)
                    .ipAddress(HttpRequestUtils.resolveClientIp(httpRequest))
                    .build();

            unsubscribeRepository.save(record);

            log.info("[Unsubscribe] email={} unsubscribed from campaign=[{}] (activityId={})",
                    emailId, campaign.getCampaignCode(), activityId);
        } else {
            log.info("[Unsubscribe] email={} was already unsubscribed from campaign=[{}]. No-op.",
                    emailId, campaign.getCampaignCode());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildPage("Unsubscribed Successfully",
                        "You have been successfully unsubscribed from <strong>" + escapeHtml(campaign.getName())
                                + "</strong>. You will no longer receive emails from this campaign.",
                        true));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Optional<Campaign> resolveCampaign(Long campaignId, String campaignCode) {
        if (campaignId != null) {
            Optional<Campaign> byId = campaignService.findById(campaignId);
            if (byId.isPresent()) return byId;
        }
        if (campaignCode != null && !campaignCode.isBlank()) {
            return campaignService.findByCampaignCode(campaignCode);
        }
        return Optional.empty();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    /**
     * Returns a self-contained, styled HTML confirmation page.
     * No external resources are loaded so it works in any email client's browser.
     */
    private String buildPage(String title, String message, boolean success) {
        String iconColor  = success ? "#10b981" : "#ef4444";
        String iconSymbol = success ? "&#10003;" : "&#9888;";
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head>" +
               "<meta charset=\"UTF-8\"/>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>" +
               "<title>" + escapeHtml(title) + "</title>" +
               "<style>" +
               "  *{box-sizing:border-box;margin:0;padding:0}" +
               "  body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen,Ubuntu,sans-serif;" +
               "       background:#f0f4f8;min-height:100vh;display:flex;align-items:center;justify-content:center;}" +
               "  .card{background:#fff;border-radius:16px;padding:48px 40px;max-width:480px;width:90%;" +
               "         box-shadow:0 4px 24px rgba(0,0,0,.08);text-align:center;}" +
               "  .icon{width:72px;height:72px;border-radius:50%;background:" + iconColor + "22;" +
               "        display:flex;align-items:center;justify-content:center;" +
               "        font-size:36px;color:" + iconColor + ";margin:0 auto 24px;}" +
               "  h1{font-size:24px;font-weight:700;color:#1a202c;margin-bottom:12px;}" +
               "  p{font-size:16px;color:#4a5568;line-height:1.6;}" +
               "  .footer{margin-top:32px;font-size:13px;color:#a0aec0;}" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class=\"card\">" +
               "  <div class=\"icon\">" + iconSymbol + "</div>" +
               "  <h1>" + escapeHtml(title) + "</h1>" +
               "  <p>" + message + "</p>" +
               "  <p class=\"footer\">You can close this tab now.</p>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
}
