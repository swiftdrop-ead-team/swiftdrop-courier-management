package com.swiftdrop.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PricingServiceApplicationTests {

    @Test
    @DisplayName("Application context loads successfully with in-memory database")
    void contextLoads() {
        assertTrue(true, "Spring application context initialized successfully");
    }
}
