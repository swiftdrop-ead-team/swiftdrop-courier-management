package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full details of a dispatch assignment returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAssignmentResponse {

    // Unique assignment database ID
    private Long id;

    // Associated parcel ID
    private String parcelId;

    // Assigned courier database ID
    private Long courierId;

    // Assigned courier full name
    private String courierName;

    // Courier contact phone number
    private String courierPhone;

    // Public parcel tracking code
    private String trackingNumber;

    // Origin pickup address
    private String pickupAddress;

    // Destination address
    private String deliveryAddress;

    // Customer recipient name
    private String recipientName;

    // Customer recipient contact phone
    private String recipientPhone;

    // Current delivery lifecycle status
    private AssignmentStatus status;

    // Reason why delivery failed, if applicable
    private String failureReason;

    // Delivery or dispatcher notes
    private String notes;

    // Timestamp when assignment was made
    private LocalDateTime assignedAt;

    // Timestamp when delivery finished
    private LocalDateTime completedAt;

    // Embedded Proof of Delivery details, if already delivered
    private ProofOfDeliveryResponse proofOfDelivery;

    // Audit creation timestamp
    private LocalDateTime createdAt;
}
