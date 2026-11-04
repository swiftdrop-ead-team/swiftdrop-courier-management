package com.swiftdrop.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Proof of Delivery confirmation payload returned to clients and tracking systems.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryResponse {

    // Unique POD database ID
    private Long id;

    // Associated assignment ID
    private Long assignmentId;

    // Name of the recipient who signed / accepted
    private String recipientName;

    // Recipient NIC number
    private String recipientNic;

    // Digital signature image link
    private String signatureUrl;

    // Parcel doorstep photo link
    private String photoUrl;

    // Delivery notes
    private String deliveryNotes;

    // Timestamp when delivery was completed and POD recorded
    private LocalDateTime deliveredAt;
}
