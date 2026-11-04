package com.swiftdrop.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryRequest {

    @NotBlank(message = "Recipient name is required for proof of delivery")
    private String recipientName;

    private String recipientNic;

    private String signatureUrl;

    private String photoUrl;

    private String deliveryNotes;
}

