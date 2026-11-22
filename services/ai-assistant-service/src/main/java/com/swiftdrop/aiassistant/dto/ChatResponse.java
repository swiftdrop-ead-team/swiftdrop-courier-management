package com.swiftdrop.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private Long inputTokens;
    private Long outputTokens;
    private Double estimatedCostUsd;

    public ChatResponse(String response) {
        this(response, null, null, null);
    }
}