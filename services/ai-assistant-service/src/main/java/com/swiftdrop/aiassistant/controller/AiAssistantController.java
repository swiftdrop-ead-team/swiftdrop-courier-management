package com.swiftdrop.aiassistant.controller;

import com.swiftdrop.aiassistant.dto.ChatRequest;
import com.swiftdrop.aiassistant.dto.ChatResponse;
import com.swiftdrop.aiassistant.security.JwtService;
import com.swiftdrop.aiassistant.service.AiAssistantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final JwtService jwtService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatWithAssistant(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        // Extract user identity and roles from JWT to enforce RBAC securely
        String authHeader = httpRequest.getHeader("Authorization");
        String userEmail = "anonymous";
        List<String> roles = List.of("CUSTOMER");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userEmail = jwtService.extractUsername(token);
                roles = jwtService.extractRoles(token);
            } catch (Exception ignored) {}
        }

        ChatResponse response = aiAssistantService.processQuery(request.getMessage(), userEmail, roles);
        return ResponseEntity.ok(response);
    }
}