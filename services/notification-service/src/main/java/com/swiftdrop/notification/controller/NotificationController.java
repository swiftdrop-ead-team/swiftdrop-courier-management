package com.swiftdrop.notification.controller;

import com.swiftdrop.notification.dto.ApiResponse;
import com.swiftdrop.notification.dto.NotificationResponse;
import com.swiftdrop.notification.dto.SendNotificationRequest;
import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller exposing endpoints for querying notification audit history,
 * single log inspection, and on-demand manual notification triggers for evaluators.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "Endpoints for notification history, audit trail, and live demo testing")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves notification history, optionally filtered by parcelId.
     */
    @GetMapping("/history")
    @Operation(summary = "Get notification history", description = "Fetch all notification logs or filter by parcelId")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getHistory(
            @RequestParam(required = false) String parcelId) {

        List<NotificationLog> logs;
        if (parcelId != null && !parcelId.isBlank()) {
            logs = notificationService.getHistoryByParcelId(parcelId);
        } else {
            logs = notificationService.getAllHistory();
        }

        List<NotificationResponse> response = logs.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response, "Retrieved " + response.size() + " notification records"));
    }

    /**
     * Retrieves a single notification log by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Fetch detailed delivery audit for a specific notification record")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long id) {
        NotificationLog logEntry = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.fromEntity(logEntry)));
    }

    /**
     * Triggers a notification on demand for manual evaluator demonstration.
     */
    @PostMapping("/send")
    @Operation(summary = "Send on-demand notification", description = "Trigger a manual notification email for testing/demo without needing RabbitMQ")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendManualNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Manual notification request received for parcelId: {}, type: {}",
                request.getParcelId(), request.getNotificationType());

        NotificationLog result = notificationService.sendManualNotification(
                request.getParcelId(),
                request.getTrackingNumber(),
                request.getRecipientEmail(),
                request.getRecipientName(),
                request.getNotificationType(),
                request.getCustomMessage()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(NotificationResponse.fromEntity(result), "Notification dispatched successfully"));
    }
}

