package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.ChatResponse;
import com.swiftdrop.aiassistant.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiAssistantService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    @Value("${services.tracking.url:http://tracking-service:8086}")
    private String trackingServiceUrl;

    private final RestClient restClient;

    private final LinkedHashMap<String, CachedResponse> responseCache = new LinkedHashMap<>(64, 0.75f, true);

    private record CachedResponse(String text, long expiresAt) {}

    public AiAssistantService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

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
        return processQuery(userMessage, userEmail, roles, authHeader, List.of());
    }
    public ChatResponse processQuery(String userMessage, String userEmail, List<String> roles, String authHeader,
                                     List<ChatRequest.ConversationMessage> history) {
        String localAnswer = getLocalAnswer(userMessage);
        if (localAnswer != null) {
            return new ChatResponse(localAnswer);
        }

        String groundedContext = gatherGroundedBackendData(userMessage, authHeader);
        if (!groundedContext.isBlank()) {
            return new ChatResponse(groundedContext);
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new ChatResponse("AI Assistant is in offline mode (Gemini API key not configured). " +
                    "You can ask for a delivery charge or track a parcel by ID.");
        }

        String cacheKey = normalizeForCache(userMessage);
        CachedResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            return new ChatResponse(cached.text());
        }

        try {
            String model = geminiModel == null || geminiModel.isBlank() ? "gemini-3.6-flash" : geminiModel.trim();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                    + ":generateContent?key=" + geminiApiKey;

            StringBuilder prompt = new StringBuilder(
                    "You are SwiftDrop AI, a concise courier assistant. Never invent tracking or prices. "
                    + "Answer only the user's courier question.");
            if (history != null) {
                prompt.append("\nRecent conversation:");
                for (ChatRequest.ConversationMessage message : history) {
                    if (message.getRole() != null && message.getText() != null) {
                        prompt.append("\n").append(message.getRole()).append(": ")
                                .append(message.getText().replaceAll("\\s+", " ").trim());
                    }
                }
            }
            prompt.append("\nUser: ").append(userMessage);

            Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString())))));
            requestBody.put("generationConfig", Map.of(
                    "temperature", 0.2,
                    "maxOutputTokens", 256,
                    "candidateCount", 1
            ));

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String answer = extractTextFromGeminiResponse(response);
            putCachedResponse(cacheKey, answer);
            Map<String, Object> usage = extractUsage(response);
            long inputTokens = ((Number) usage.getOrDefault("input", 0)).longValue();
            long outputTokens = ((Number) usage.getOrDefault("output", 0)).longValue();
            double estimatedCost = (inputTokens * 0.30 / 1_000_000.0)
                    + (outputTokens * 2.50 / 1_000_000.0);
            log.info("Gemini usage: inputTokens={} outputTokens={} estimatedCostUsd={}",
                    inputTokens, outputTokens, String.format("%.8f", estimatedCost));
            return new ChatResponse(answer, inputTokens, outputTokens, estimatedCost);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "The AI provider is busy. Please wait before retrying.");
            }
            log.warn("AI provider returned HTTP {}", e.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI provider request failed. Please try again later.");
        } catch (Exception e) {
            log.warn("AI provider request failed ({})", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI provider request failed. Please try again later.");
        }
    }

    private String getLocalAnswer(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("fragile") || lower.contains("pack")) {
            return "Use a sturdy box, wrap the item individually, fill empty space, and mark the parcel \"Fragile\". "
                    + "Remove old labels and keep liquids in sealed leak-proof packaging.";
        }
        if (lower.contains("before booking") || lower.contains("prepare") || lower.contains("check")) {
            return "Before booking, confirm the destination address and phone number, parcel dimensions and weight, "
                    + "contents, packing strength, and the required delivery speed.";
        }
        if (lower.contains("out for delivery") || lower.contains("delivery stage")) {
            return "\"Out for delivery\" means the courier is assigned and the parcel is on the final route to the destination.";
        }
        return null;
    }

    private String normalizeForCache(String message) {
        return message.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private synchronized CachedResponse getCachedResponse(String key) {
        CachedResponse cached = responseCache.get(key);
        if (cached == null) return null;
        if (cached.expiresAt() < System.currentTimeMillis()) {
            responseCache.remove(key);
            return null;
        }
        return cached;
    }

    private synchronized void putCachedResponse(String key, String answer) {
        while (responseCache.size() >= 200) {
            String oldest = responseCache.entrySet().iterator().next().getKey();
            responseCache.remove(oldest);
        }
        responseCache.put(key, new CachedResponse(answer, System.currentTimeMillis() + 5 * 60 * 1000));
    }

    private Map<String, Object> extractUsage(Map<?, ?> response) {
        Map<String, Object> usage = new java.util.LinkedHashMap<>();
        try {
            Map<?, ?> metadata = (Map<?, ?>) response.get("usageMetadata");
            usage.put("input", metadata.get("promptTokenCount") == null ? 0 : metadata.get("promptTokenCount"));
            usage.put("output", metadata.get("candidatesTokenCount") == null ? 0 : metadata.get("candidatesTokenCount"));
        } catch (Exception ignored) {
            usage.put("input", 0);
            usage.put("output", 0);
        }
        return usage;
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
