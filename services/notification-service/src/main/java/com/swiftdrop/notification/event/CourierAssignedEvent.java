package com.swiftdrop.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event message consumed from RabbitMQ (dispatch.courier.assigned)
 * published when a courier is assigned to a parcel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourierAssignedEvent implements Serializable {

    private String eventId;

    @Builder.Default
    private String eventType = "COURIER_ASSIGNED";

    private Long assignmentId;
    private String parcelId;
    private Long courierId;
    private String courierName;
    private String courierPhone;
    private String courierEmail;
    private String trackingNumber;
    private String pickupAddress;
    private String deliveryAddress;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String status;
    private LocalDateTime timestamp;
}

