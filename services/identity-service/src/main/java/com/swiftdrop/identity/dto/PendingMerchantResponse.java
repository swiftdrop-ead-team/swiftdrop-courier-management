package com.swiftdrop.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingMerchantResponse {

    private UUID id;
    private UUID userId;
    private String businessName;
    private String contactPerson;
    private String email;
    private String phoneNumber;
    private String businessAddress;
    private String approvalStatus;
    private String notes;
    private Instant registeredAt;
}
