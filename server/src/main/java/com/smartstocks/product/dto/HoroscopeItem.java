package com.smartstocks.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HoroscopeItem {
    private String title;
    private String description;
    private String link;
    private String pubDate;
    private String zodiacName;
    private String imageUrl;
}
