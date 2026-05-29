package com.smartstocks.product.service;

import com.smartstocks.product.dto.ShortLinkDto;
import com.smartstocks.product.models.ShortLink;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IShortLinkService {

    String shortenLink(String originalUrl, Duration ttl);

    ShortLinkDto getLinkStatistics(String shortId);

    boolean deleteLink(String shortId);

    List<ShortLinkDto> getAllLinks();

    Optional<ShortLink> getLinkByShortId(String shortId);

    Optional<String> resolveOriginalUrl(String shortId);

    CompletableFuture<Void> incrementClickCountAsync(String shortId);
}
