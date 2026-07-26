package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMetricsDto {
    private long totalSends;
    private long totalOpens;
    private long totalClicks;
}
