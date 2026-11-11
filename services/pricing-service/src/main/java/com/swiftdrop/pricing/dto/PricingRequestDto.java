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
 *   - physicalWeightKg must be > 0.
 *   - lengthCm, widthCm, heightCm must be > 0.
 *   - distanceKm must be >= 0.
 *   - promoCode, orderReference, customerId are optional.
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
    private String originZone;

    @NotBlank(message = "Destination zone is required")
    private String destinationZone;

    @NotNull(message = "Physical weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Physical weight must be greater than zero")
    private BigDecimal physicalWeightKg;

    @NotNull(message = "Length is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Length must be greater than zero")
    private BigDecimal lengthCm;

    @NotNull(message = "Width is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than zero")
    private BigDecimal widthCm;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than zero")
    private BigDecimal heightCm;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Distance cannot be negative")
    private BigDecimal distanceKm;

    // Optional promotional fields
    private String promoCode;
    private String orderReference;
    private String customerId;
}
