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
public class DeliveryStatusUpdatedEvent implements Serializable {
    @Builder.Default
    private String eventType = "DELIVERY_STATUS_UPDATED";
    private Long assignmentId;
    private String parcelId;
    private String trackingNumber;
    private Long courierId;
    private String previousStatus;
    private String newStatus;
    private String failureReason;
    private String notes;
    private LocalDateTime timestamp;
}
