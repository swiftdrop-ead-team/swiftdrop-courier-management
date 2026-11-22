package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssistantServiceTest {

    private final AiAssistantService aiAssistantService = new AiAssistantService();

    @Test
    void processQuery_returnsGroundedPricingForKandy() {
        ChatResponse response = aiAssistantService.processQuery(
                "What is the delivery charge to Kandy?",
                "customer@swiftdrop.com",
                List.of("ROLE_CUSTOMER"),
                "Bearer fake-token"
        );

        assertTrue(response.getResponse().contains("LKR 550"),
                "Expected grounded Kandy rate (LKR 550), got: " + response.getResponse());
    }

    @Test
    void processQuery_blocksPrivateTrackingWithoutAuthorization() {
        ChatResponse response = aiAssistantService.processQuery(
                "Where is my parcel SD-1001?",
                "anonymous",
                List.of("ROLE_CUSTOMER"),
                null
        );

        assertTrue(response.getResponse().toLowerCase().contains("authorization is required"),
                "Expected auth protection message, got: " + response.getResponse());
    }
}
