package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLinkDto {

    private String shortLink;
    private String shortId;
    private String originalUrl;
    private int clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
