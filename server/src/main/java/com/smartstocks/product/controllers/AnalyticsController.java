package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.EmailMetricsDto;
import com.smartstocks.product.repository.CampaignSegmentUserRepository;
import com.smartstocks.product.repository.EmailOpenEventRepository;
import com.smartstocks.product.repository.LinkClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {

    private final CampaignSegmentUserRepository campaignSegmentUserRepository;
    private final EmailOpenEventRepository emailOpenEventRepository;
    private final LinkClickEventRepository linkClickEventRepository;

    @GetMapping("/email-metrics")
    public ResponseEntity<EmailMetricsDto> getEmailMetrics() {
        long totalSends = campaignSegmentUserRepository.countTotalEmailSends();
        long totalOpens = emailOpenEventRepository.count();
        long totalClicks = linkClickEventRepository.count();

        EmailMetricsDto metrics = EmailMetricsDto.builder()
                .totalSends(totalSends)
                .totalOpens(totalOpens)
                .totalClicks(totalClicks)
                .build();

        return ResponseEntity.ok(metrics);
    }
}
