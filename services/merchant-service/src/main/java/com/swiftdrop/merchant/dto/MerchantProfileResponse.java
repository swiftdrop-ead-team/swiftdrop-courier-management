package com.swiftdrop.merchant.dto;

import com.swiftdrop.merchant.entity.MerchantApprovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MerchantProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String businessName,
        String contactPerson,
        String phoneNumber,
        String businessAddress,
        MerchantApprovalStatus approvalStatus,
        String reviewNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
