package com.swiftdrop.dispatch.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event message published to RabbitMQ when a parcel delivery status changes
 * (e.g., PICKED_UP, IN_TRANSIT, DELIVERED, FAILED).
 * Consumed by: Tracking Service, Notification Service, Merchant Service webhooks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusUpdatedEvent implements Serializable {

    // Event type classifier
    @Builder.Default
    private String eventType = "DELIVERY_STATUS_UPDATED";

    // Assignment ID
    private Long assignmentId;

    // Associated parcel ID
    private String parcelId;

    // Public tracking code
    private String trackingNumber;

    // Courier handling the parcel
    private Long courierId;

    // Status prior to this transition
    private String previousStatus;

    // New updated lifecycle status
    private String newStatus;

    // Failure reason if marked FAILED
    private String failureReason;

    // Notes associated with status change
    private String notes;

    // Timestamp when status changed
    private LocalDateTime timestamp;
}
