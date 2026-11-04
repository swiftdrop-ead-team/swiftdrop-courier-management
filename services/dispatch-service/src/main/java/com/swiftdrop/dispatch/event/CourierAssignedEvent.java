package com.swiftdrop.dispatch.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierAssignedEvent implements Serializable {
    @Builder.Default
    private String eventType = "COURIER_ASSIGNED";
    private Long assignmentId;
    private String parcelId;
    private Long courierId;
    private String courierName;
    private String courierPhone;
    private String trackingNumber;
    private String pickupAddress;
    private String deliveryAddress;
    private String recipientName;
    private String recipientPhone;
    private String status;
    private LocalDateTime timestamp;
}
