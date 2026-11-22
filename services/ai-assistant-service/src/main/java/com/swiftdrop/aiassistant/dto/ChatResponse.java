package com.swiftdrop.aiassistant.dto;

import lombok.Data;

@Data
public class ChatResponse {
    private String response;
    private Long inputTokens;
    private Long outputTokens;
    private Double estimatedCostUsd;

    public ChatResponse(String response, Long inputTokens, Long outputTokens, Double estimatedCostUsd) {
        this.response = response;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.estimatedCostUsd = estimatedCostUsd;
    }

    public ChatResponse(String response) {
        this(response, null, null, null);
    }
}
