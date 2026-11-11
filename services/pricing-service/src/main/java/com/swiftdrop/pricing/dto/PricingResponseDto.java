package com.swiftdrop.pricing.dto;

import com.swiftdrop.pricing.entity.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned to the client after a pricing calculation.
 * Fields correspond one‑to‑one with the columns of the {@code price_quotes} table.
 * No extra fields are introduced – the DTO merely transports the persisted data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingResponseDto {

    private UUID id;
    private ServiceType serviceType;
    private String originZone;
    private String destinationZone;
    private BigDecimal physicalWeightKg;
    private BigDecimal volumetricWeightKg;
    private BigDecimal billableWeightKg;
    private BigDecimal distanceKm;

    private BigDecimal baseFare;
    private BigDecimal weightCharge;
    private BigDecimal distanceCharge;
    private BigDecimal urgencyMultiplier;
    private BigDecimal zoneMultiplier;
    private BigDecimal fuelSurchargeAmount;
    private BigDecimal merchantDiscountAmount;
    private BigDecimal promoDiscountAmount;
    private BigDecimal finalFare;
    private String currency;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
