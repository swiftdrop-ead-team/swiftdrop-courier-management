package com.swiftdrop.dispatch.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event message published to RabbitMQ (exchange: dispatch.exchange)
 * when a courier is assigned to a parcel.
 * Consumed by: Notification Service (sends SMS/Email to courier and customer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierAssignedEvent implements Serializable {
    private String courierUserId;
    private String courierEmail;

    // Event type classifier
    @Builder.Default
    private String eventType = "COURIER_ASSIGNED";

    // Assignment ID in Dispatch Database
    private Long assignmentId;

    // Parcel ID
    private String parcelId;

    // Courier database ID
    private Long courierId;

    // Courier name
    private String courierName;

    // Courier phone number
    private String courierPhone;

    // Parcel tracking number
    private String trackingNumber;

    // Origin pickup address
    private String pickupAddress;

    // Destination address
    private String deliveryAddress;

    // Customer recipient name
    private String recipientName;

    // Customer recipient phone
    private String recipientPhone;

    // Assignment status (ASSIGNED)
    private String status;

    // Timestamp when the assignment occurred
    private LocalDateTime timestamp;
}
