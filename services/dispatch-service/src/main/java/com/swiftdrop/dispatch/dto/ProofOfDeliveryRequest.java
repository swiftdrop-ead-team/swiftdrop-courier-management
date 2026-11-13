package com.swiftdrop.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload sent by courier mobile app or web portal to record Proof of Delivery (POD).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryRequest {

    // Name of the person who received the parcel (required)
    @NotBlank(message = "Recipient name is required for proof of delivery")
    private String recipientName;

    // National Identity Card number of recipient (optional)
    private String recipientNic;

    // URL link to digital signature capture image
    private String signatureUrl;

    // URL link to parcel photo taken upon delivery
    private String photoUrl;

    // Delivery confirmation comments (e.g., "Handed directly to owner")
    private String deliveryNotes;
}
