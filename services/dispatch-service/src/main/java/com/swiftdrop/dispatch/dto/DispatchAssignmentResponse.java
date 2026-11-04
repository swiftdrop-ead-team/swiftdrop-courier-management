package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAssignmentResponse {
    private Long id;
    private String parcelId;
    private Long courierId;
    private String courierName;
    private String courierPhone;
    private String trackingNumber;
    private String pickupAddress;
    private String deliveryAddress;
    private String recipientName;
    private String recipientPhone;
    private AssignmentStatus status;
    private String failureReason;
    private String notes;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private ProofOfDeliveryResponse proofOfDelivery;
    private LocalDateTime createdAt;
}

