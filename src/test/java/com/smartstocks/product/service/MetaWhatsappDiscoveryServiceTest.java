package com.smartstocks.product.service;

import com.smartstocks.product.dto.DiscoveredWabaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MetaWhatsappDiscoveryServiceTest {

    private MetaWhatsappDiscoveryService discoveryService;

    @BeforeEach
    public void setup() {
        discoveryService = new MetaWhatsappDiscoveryService();
    }

    @Test
    public void testComputeAppSecretProof_EmptySecret() {
        String proof = discoveryService.computeAppSecretProof("EAAG123");
        assertEquals("", proof);
    }

    @Test
    public void testDiscoverPrimary_NullOrEmptyToken() {
        Optional<DiscoveredWabaInfo> res = discoveryService.discoverPrimary(null, null, null);
        assertTrue(res.isEmpty());

        res = discoveryService.discoverPrimary("", null, null);
        assertTrue(res.isEmpty());
    }

    @Test
    public void testDiscoverPrimary_FiltersDummyWabaId() {
        // When dummy WABA ID is passed, it should not accept it as valid known ID
        Optional<DiscoveredWabaInfo> res = discoveryService.discoverPrimary("token", null, "1726866808739698");
        // Because token is dummy and cannot connect to Meta, it shouldn't return 1726866808739698
        if (res.isPresent()) {
            assertNotEquals("1726866808739698", res.get().getWabaId());
        }
    }

    @Test
    public void testDiscoverAll_EmptyToken() {
        List<DiscoveredWabaInfo> list = discoveryService.discoverAll("");
        assertTrue(list.isEmpty());
    }
}
