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
            return new ChatResponse("AI Assistant is currently offline (Gemini API key not configured). " +
                    "However, I can confirm you asked: \"" + userMessage + "\". Your role is " + roles);
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
            log.error("Error communicating with Gemini API", e);
            return new ChatResponse("Sorry, I encountered an error processing your request with the AI assistant.");
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
        return "I received an empty response from the AI model.";
    }
}