package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${services.tracking.url:http://tracking-service:8086}")
    private String trackingServiceUrl;

    private final RestClient restClient = RestClient.create();

    // Matches tracking IDs like SD-12345, TRK-001, or PKG-999
    private static final Pattern TRACKING_ID_PATTERN = Pattern.compile("\\b(SD|TRK|PKG)-[A-Za-z0-9-]+\\b", Pattern.CASE_INSENSITIVE);

    // Grounded Sri Lanka standard courier rate table (LKR)
    private static final Map<String, Integer> CITY_RATES_LKR = Map.of(
            "colombo", 350,
            "kandy", 550,
            "galle", 600,
            "negombo", 450,
            "jaffna", 850,
            "matara", 650,
            "kurunegala", 500,
            "ratnapura", 550
    );
    public ChatResponse processQuery(String userMessage, String userEmail, List<String> roles) {
        return processQuery(userMessage, userEmail, roles, null);
    }
    public ChatResponse processQuery(String userMessage, String userEmail, List<String> roles, String authHeader) {
        String groundedContext = gatherGroundedBackendData(userMessage, authHeader);

        // If no Gemini API key is configured, return the grounded backend data directly
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            if (!groundedContext.isBlank()) {
                return new ChatResponse("[Grounded Backend Data] " + groundedContext);
            }
            return new ChatResponse("AI Assistant is in offline mode (Gemini API key not configured). " +
                    "You can ask about delivery charges (e.g., 'What is the charge to Kandy?') or track a parcel by ID (e.g., 'Where is my parcel SD-1001?').");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            String prompt = "You are SwiftDrop AI, a courier assistant. " +
                    "Current user: " + userEmail + " | Roles: " + roles + ". " +
                    "STRICT RULE: Only use the Backend Data provided below for parcel status or prices. Never invent tracking statuses or prices.\n" +
                    "Backend Data: " + (groundedContext.isBlank() ? "No matching tracking/pricing record found." : groundedContext) + "\n" +
                    "User Question: " + userMessage;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return new ChatResponse(extractTextFromGeminiResponse(response));

        } catch (Exception e) {
            log.warn("AI provider request failed ({})", e.getClass().getSimpleName());
            if (!groundedContext.isBlank()) {
                return new ChatResponse(groundedContext);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI provider request failed. Please try again later.");
        }
    }

    private String gatherGroundedBackendData(String message, String authHeader) {
        StringBuilder context = new StringBuilder();
        String lower = message.toLowerCase();

        // 1. Check for Pricing / Charge queries
        if (lower.contains("charge") || lower.contains("price") || lower.contains("cost") || lower.contains("rate") || lower.contains("fee")) {
            for (Map.Entry<String, Integer> entry : CITY_RATES_LKR.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    String city = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
                    context.append(String.format("Standard delivery charge to %s is LKR %d (up to 1kg; +LKR 150 per additional kg). ", city, entry.getValue()));
                    break;
                }
            }
        }

        // 2. Check for Tracking ID and call tracking-service with the user's JWT
        Matcher matcher = TRACKING_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            String trackingId = matcher.group().toUpperCase();
            context.append(fetchTrackingStatus(trackingId, authHeader));
        } else if (lower.contains("where is my parcel") || lower.contains("track")) {
            context.append("Please include your tracking number (for example, SD-1001) so I can look up its live status from the Tracking Service.");
        }

        return context.toString().trim();
    }

    private String fetchTrackingStatus(String trackingId, String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Authorization is required to access private parcel tracking details.";
        }
        try {
            String response = restClient.get()
                    .uri(trackingServiceUrl + "/api/v1/tracking/" + trackingId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(String.class);
            return "Tracking Service record for " + trackingId + ": " + response;
        } catch (Exception e) {
            log.warn("Could not fetch tracking info for {}: {}", trackingId, e.getMessage());
            return "No active tracking record was found for parcel " + trackingId + " in the Tracking Service.";
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
