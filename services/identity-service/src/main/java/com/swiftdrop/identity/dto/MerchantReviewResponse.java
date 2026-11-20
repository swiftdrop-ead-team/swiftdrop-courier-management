package com.swiftdrop.identity.dto;

import com.swiftdrop.identity.entity.MerchantApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record MerchantReviewResponse(UUID id, String merchant, MerchantApprovalStatus decision,
                                     String notes, Instant timestamp, String actor) {}
