package com.swiftdrop.pricing.dto;

import com.swiftdrop.pricing.entity.ServiceType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * DTO representing a request to calculate a pricing quote.
 * Validation constraints follow the approved design:
 *   - serviceType, originZone, destinationZone are required.
 *   - physicalWeightKg must be > 0 and <= 999999.99 with up to 2 decimal places.
 *   - lengthCm, widthCm, heightCm must be > 0 and <= 999999.99 with up to 2 decimal places.
 *   - distanceKm must be >= 0 and <= 999999.99 with up to 2 decimal places.
 *   - promoCode, orderReference, customerId are optional with max length limits.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRequestDto {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Origin zone is required")
    @Size(max = 50, message = "Origin zone must not exceed 50 characters")
    private String originZone;

    @NotBlank(message = "Destination zone is required")
    @Size(max = 50, message = "Destination zone must not exceed 50 characters")
    private String destinationZone;

    @NotNull(message = "Physical weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Physical weight must be greater than zero")
    @DecimalMax(value = "999999.99", message = "Physical weight must not exceed 999,999.99 kg")
    @Digits(integer = 6, fraction = 2, message = "Physical weight must have at most 6 integer digits and 2 decimal places")
    private BigDecimal physicalWeightKg;

    @NotNull(message = "Length is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Length must be greater than zero")
    @DecimalMax(value = "999999.99", message = "Length must not exceed 999,999.99 cm")
    @Digits(integer = 6, fraction = 2, message = "Length must have at most 6 integer digits and 2 decimal places")
    private BigDecimal lengthCm;

    @NotNull(message = "Width is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than zero")
    @DecimalMax(value = "999999.99", message = "Width must not exceed 999,999.99 cm")
    @Digits(integer = 6, fraction = 2, message = "Width must have at most 6 integer digits and 2 decimal places")
    private BigDecimal widthCm;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than zero")
    @DecimalMax(value = "999999.99", message = "Height must not exceed 999,999.99 cm")
    @Digits(integer = 6, fraction = 2, message = "Height must have at most 6 integer digits and 2 decimal places")
    private BigDecimal heightCm;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Distance cannot be negative")
    @DecimalMax(value = "999999.99", message = "Distance must not exceed 999,999.99 km")
    @Digits(integer = 6, fraction = 2, message = "Distance must have at most 6 integer digits and 2 decimal places")
    private BigDecimal distanceKm;

    // Optional promotional & tracking fields (no @NotBlank or @NotNull)
    @Size(max = 50, message = "Promo code must not exceed 50 characters")
    private String promoCode;

    @Size(max = 100, message = "Order reference must not exceed 100 characters")
    private String orderReference;

    @Size(max = 100, message = "Customer ID must not exceed 100 characters")
    private String customerId;
}