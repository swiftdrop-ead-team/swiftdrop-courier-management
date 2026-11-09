package com.swiftdrop.pricing.controller;

import com.swiftdrop.pricing.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Basic health and status verification endpoint for the Pricing Service.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Health & Status", description = "Service heartbeat and diagnostic verification")
public class HealthCheckController {

    @GetMapping("/health")
    @Operation(summary = "Check Pricing Service status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> status = Map.of(
                "service", "pricing-service",
                "status", "UP",
                "member", "Member 6",
                "port", 8085
        );
        return ResponseEntity.ok(ApiResponse.ok("Pricing Service is operational", status));
    }
}
