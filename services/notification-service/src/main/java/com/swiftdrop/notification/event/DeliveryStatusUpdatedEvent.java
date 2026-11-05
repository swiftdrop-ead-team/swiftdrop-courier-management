package com.swiftdrop.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event message consumed from RabbitMQ (dispatch.status.updated)
 * published when a parcel delivery status changes (e.g., OUT_FOR_DELIVERY, DELIVERED, FAILED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryStatusUpdatedEvent implements Serializable {

    private String eventId;

    @Builder.Default
    private String eventType = "DELIVERY_STATUS_UPDATED";

    private Long assignmentId;
    private String parcelId;
    private String trackingNumber;
    private Long courierId;
    private String courierName;
    private String previousStatus;
    private String newStatus;
    private String failureReason;
    private String notes;
    private String recipientEmail;
    private String recipientName;
    private LocalDateTime timestamp;
}

