package com.smartstocks.product.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShortLinkUiController {

    @Value("${app.short-link.base-url}")
    private String shortLinkBaseUrl;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @GetMapping("/short-links")
    public String homePage(Model model) {
        String baseUrl = shortLinkBaseUrl.endsWith("/") ? shortLinkBaseUrl : shortLinkBaseUrl + "/";
        model.addAttribute("shortLinkBaseUrl", baseUrl);
        model.addAttribute("apiLinksUrl", "/api/links");
        model.addAttribute("apiCampaignsUrl", "/api/campaigns");
        model.addAttribute("googleClientId", googleClientId);
        return "short-links/index";
    }
}
