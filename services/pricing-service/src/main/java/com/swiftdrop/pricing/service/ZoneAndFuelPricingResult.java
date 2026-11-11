package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value object holding the results of zone multiplier resolution,
 * zone-adjusted delivery fare, and floating fuel surcharge calculation.
 */
@Getter
@ToString
@AllArgsConstructor
@Builder
public class ZoneAndFuelPricingResult {

    private final DeliveryZone originZone;
    private final DeliveryZone destinationZone;
    private final BigDecimal zoneMultiplier;
    private final BigDecimal zoneAdjustedFare;
    private final BigDecimal fuelSurchargeRatePercentage;
    private final BigDecimal fuelSurchargeAmount;
    private final BigDecimal subtotalWithFuel;
}
