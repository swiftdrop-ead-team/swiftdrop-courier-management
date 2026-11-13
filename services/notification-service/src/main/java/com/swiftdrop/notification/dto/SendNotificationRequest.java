package com.swiftdrop.notification.dto;

import com.swiftdrop.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for manual notification dispatch triggers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to trigger a notification dispatch on demand")
public class SendNotificationRequest {

    @NotBlank(message = "parcelId is required")
    @Schema(description = "Associated parcel ID", example = "PRC-2026-8801")
    private String parcelId;

    @Schema(description = "Public tracking code", example = "SWIFT-LK-9988")
    private String trackingNumber;

    @NotBlank(message = "recipientEmail is required")
    @Email(message = "Valid recipient email address is required")
    @Schema(description = "Recipient email address", example = "customer@swiftdrop.io")
    private String recipientEmail;

    @Schema(description = "Recipient full name", example = "Alice Fernando")
    private String recipientName;

    @NotNull(message = "notificationType is required")
    @Schema(description = "Milestone notification type", example = "OUT_FOR_DELIVERY")
    private NotificationType notificationType;

    @Schema(description = "Custom message or failure notes", example = "Your driver is 5 minutes away.")
    private String customMessage;
}

