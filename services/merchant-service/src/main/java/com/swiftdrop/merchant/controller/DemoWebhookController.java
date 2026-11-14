package com.swiftdrop.merchant.controller;

import com.swiftdrop.merchant.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/v1/merchant/demo")
public class DemoWebhookController {

    private final AtomicLong receivedCount = new AtomicLong();

    @PostMapping("/webhook")
    public ApiResponse<Map<String, Object>> receive(
            @RequestHeader(value = "X-SwiftDrop-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-SwiftDrop-Signature", required = false) String signature) {
        long count = receivedCount.incrementAndGet();
        return ApiResponse.ok("Demo webhook received", Map.of(
                "receiptId", UUID.randomUUID().toString(),
                "receivedCount", count,
                "eventId", eventId == null ? "" : eventId,
                "signaturePresent", signature != null && signature.startsWith("sha256=")));
    }
}
