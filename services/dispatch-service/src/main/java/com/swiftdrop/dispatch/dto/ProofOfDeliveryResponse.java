package com.swiftdrop.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryResponse {
    private Long id;
    private Long assignmentId;
    private String recipientName;
    private String recipientNic;
    private String signatureUrl;
    private String photoUrl;
    private String deliveryNotes;
    private LocalDateTime deliveredAt;
}

