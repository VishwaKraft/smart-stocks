package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.ShortLinkDto;
import com.smartstocks.product.models.ShortLink;
import com.smartstocks.product.repository.ShortLinkRepository;
import com.smartstocks.product.service.IShortLinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ShortLinkServiceImpl implements IShortLinkService {

    private static final int SHORT_ID_LENGTH = 6;

    private final ShortLinkRepository shortLinkRepository;
    private final String baseUrl;

    public ShortLinkServiceImpl(
            ShortLinkRepository shortLinkRepository,
            @Value("${app.short-link.base-url}") String baseUrl) {
        this.shortLinkRepository = shortLinkRepository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Override
    @Transactional
    public String shortenLink(String originalUrl, Duration ttl) {
        String shortId;
        do {
            shortId = generateRandomShortId();
        } while (shortLinkRepository.existsByShortId(shortId));

        LocalDateTime expiresAt = ttl == null ? null : LocalDateTime.now().plus(ttl);

        ShortLink link = new ShortLink();
        link.setOriginalUrl(originalUrl);
        link.setShortId(shortId);
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(expiresAt);
        link.setClickCount(0);
        shortLinkRepository.save(link);

        return baseUrl + shortId;
    }

    @Override
    public ShortLinkDto getLinkStatistics(String shortId) {
        return shortLinkRepository.findByShortId(shortId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean deleteLink(String shortId) {
        return shortLinkRepository.findByShortId(shortId)
                .map(link -> {
                    shortLinkRepository.delete(link);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<ShortLinkDto> getAllLinks() {
        return shortLinkRepository.findAllByOrderByClickCountDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ShortLink> getLinkByShortId(String shortId) {
        return shortLinkRepository.findByShortId(shortId);
    }

    @Override
    public Optional<String> resolveOriginalUrl(String shortId) {
        return shortLinkRepository.findByShortId(shortId)
                .map(ShortLink::getOriginalUrl);
    }

    @Override
    @Async("clickTrackingExecutor")
    public CompletableFuture<Void> incrementClickCountAsync(String shortId) {
        shortLinkRepository.incrementClickCountByShortId(shortId);
        return CompletableFuture.completedFuture(null);
    }

    private ShortLinkDto toDto(ShortLink link) {
        return ShortLinkDto.builder()
                .shortLink(baseUrl + link.getShortId())
                .shortId(link.getShortId())
                .originalUrl(link.getOriginalUrl())
                .clickCount(link.getClickCount())
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .build();
    }

    private String generateRandomShortId() {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(SHORT_ID_LENGTH);
        for (int i = 0; i < SHORT_ID_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
