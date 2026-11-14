package com.swiftdrop.notification.dto;

import com.swiftdrop.notification.entity.NotificationChannel;
import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationStatus;
import com.swiftdrop.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a notification audit record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private LocalDateTime readAt;
    private String eventId;
    private String parcelId;
    private String trackingNumber;
    private String recipientEmail;
    private String recipientName;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String subject;
    private NotificationStatus deliveryStatus;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public static NotificationResponse fromEntity(NotificationLog entity) {
        if (entity == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(entity.getId()).readAt(entity.getReadAt())
                .eventId(entity.getEventId())
                .parcelId(entity.getParcelId())
                .trackingNumber(entity.getTrackingNumber())
                .recipientEmail(entity.getRecipientEmail())
                .recipientName(entity.getRecipientName())
                .notificationType(entity.getNotificationType())
                .channel(entity.getChannel())
                .subject(entity.getSubject())
                .deliveryStatus(entity.getDeliveryStatus())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}

