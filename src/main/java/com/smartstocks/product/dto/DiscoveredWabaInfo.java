package com.smartstocks.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredWabaInfo {
    private String wabaId;
    private String wabaName;
    private String phoneNumberId;
    private String displayPhoneNumber;
    private String verifiedName;
}
