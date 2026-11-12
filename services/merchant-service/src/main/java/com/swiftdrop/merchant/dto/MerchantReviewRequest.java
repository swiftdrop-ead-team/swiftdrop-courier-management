package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantReviewRequest {

    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String notes;
}
