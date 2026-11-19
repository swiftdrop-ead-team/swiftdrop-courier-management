package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestClient restClient = RestClient.create();

    public ChatResponse processQuery(String userMessage, String userEmail, List<String> roles) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "AI Assistant is unavailable: provider credentials are not configured");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            // Construct payload for Gemini API
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", "You are SwiftDrop AI, a helpful logistics assistant for a courier management system. " +
                                            "The current user is " + userEmail + " with roles " + roles + ". " +
                                            "Answer their delivery or pricing query accurately: " + userMessage)
                            ))
                    )
            );

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // Extract text from Gemini's response structure
            String aiAnswer = extractTextFromGeminiResponse(response);
            return new ChatResponse(aiAnswer);

        } catch (Exception e) {
            log.warn("AI provider request failed ({})", e.getClass().getSimpleName());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "AI provider request failed. Please try again later.");
        }
    }

    private String extractTextFromGeminiResponse(Map<?, ?> response) {
        try {
            if (response != null && response.containsKey("candidates")) {
                var candidates = (List<?>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    var firstCandidate = (Map<?, ?>) candidates.get(0);
                    var content = (Map<?, ?>) firstCandidate.get("content");
                    var parts = (List<?>) content.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        var firstPart = (Map<?, ?>) parts.get(0);
                        return (String) firstPart.get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response structure", e);
        }
        throw new IllegalStateException("AI provider returned no answer");
    }
}