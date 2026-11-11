package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import com.swiftdrop.pricing.exception.FuelRateNotFoundException;
import com.swiftdrop.pricing.repository.FuelSurchargeIndexRepository;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Service managing floating fuel surcharge index retrieval and distance-based surcharge calculation.
 */
@Service
@Transactional(readOnly = true)
public class FuelSurchargeService {

    private final FuelSurchargeIndexRepository fuelSurchargeIndexRepository;
    private final Clock clock;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Autowired
    public FuelSurchargeService(
            FuelSurchargeIndexRepository fuelSurchargeIndexRepository,
            @Autowired(required = false) Clock clock) {
        this.fuelSurchargeIndexRepository = fuelSurchargeIndexRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public FuelSurchargeService(FuelSurchargeIndexRepository fuelSurchargeIndexRepository) {
        this(fuelSurchargeIndexRepository, Clock.systemDefaultZone());
    }

    /**
     * Retrieves the currently active floating fuel surcharge index record effective as of the given timestamp.
     * Strictly throws {@link FuelRateNotFoundException} when no effective active record exists.
     *
     * @param evaluationTime the timestamp to evaluate effectiveness against (defaults to now if null)
     * @return the active {@link FuelSurchargeIndex}
     * @throws FuelRateNotFoundException if no active rate is effective on or before evaluationTime
     */
    public FuelSurchargeIndex getActiveFuelRate(LocalDateTime evaluationTime) {
        LocalDateTime cutoff = evaluationTime != null ? evaluationTime : LocalDateTime.now(clock);

        return fuelSurchargeIndexRepository.findCurrentActive(cutoff)
                .orElseThrow(() -> new FuelRateNotFoundException(
                        "No active fuel surcharge rate found effective on or before: " + cutoff
                ));
    }

    /**
     * Calculates the fuel surcharge amount based on the raw distance charge.
     * Formula (Option 2A - Raw Distance Charge Basis):
     * <pre>
     * fuelSurchargeAmount = distanceCharge * (ratePercentage / 100)
     * </pre>
     *
     * @param distanceCharge the unadjusted raw distance charge from the pricing strategy
     * @param ratePercentage the floating fuel surcharge index percentage
     * @return the calculated fuel surcharge amount rounded to 2 decimal places with HALF_UP
     */
    public BigDecimal calculateFuelSurcharge(BigDecimal distanceCharge, BigDecimal ratePercentage) {
        if (distanceCharge == null || ratePercentage == null) {
            throw new IllegalArgumentException("Distance charge and rate percentage cannot be null");
        }
        if (distanceCharge.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distance charge cannot be negative");
        }
        if (ratePercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fuel surcharge rate percentage cannot be negative");
        }

        return distanceCharge
                .multiply(ratePercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the complete Zone and Fuel Surcharge pricing step in the exact approved order:
     * <ol>
     *   <li>Strategy subtotal already has urgency multiplier applied once.</li>
     *   <li>Apply destination zone multiplier once to the strategy subtotal: {@code zoneAdjustedFare}.</li>
     *   <li>Calculate fuel surcharge using raw distance charge (Option 2A): {@code fuelSurchargeAmount}.</li>
     *   <li>Add fuel surcharge to zone-adjusted fare: {@code subtotalWithFuel}.</li>
     * </ol>
     *
     * @param strategyResult  the strategy output containing subtotalFare and distanceCharge
     * @param originZone      the validated origin delivery zone
     * @param destinationZone the validated destination delivery zone
     * @param evaluationTime  the timestamp for fuel rate effectiveness (nullable, defaults to now)
     * @return the computed {@link ZoneAndFuelPricingResult}
     */
    public ZoneAndFuelPricingResult calculateZoneAndFuelPricing(
            StrategyPricingResult strategyResult,
            DeliveryZone originZone,
            DeliveryZone destinationZone,
            LocalDateTime evaluationTime) {

        if (strategyResult == null) {
            throw new IllegalArgumentException("Strategy pricing result cannot be null");
        }
        if (destinationZone == null) {
            throw new IllegalArgumentException("Destination zone cannot be null");
        }

        // 1. Destination zone multiplier
        BigDecimal zoneMultiplier = destinationZone.getBaseZoneMultiplier();

        // 2. Apply zone multiplier once to strategy subtotal
        BigDecimal zoneAdjustedFare = strategyResult.getSubtotalFare()
                .multiply(zoneMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // 3. Resolve active fuel rate & compute fuel surcharge using raw distance charge
        FuelSurchargeIndex activeFuelIndex = getActiveFuelRate(evaluationTime);
        BigDecimal fuelSurchargeAmount = calculateFuelSurcharge(
                strategyResult.getDistanceCharge(),
                activeFuelIndex.getRatePercentage()
        );

        // 4. Add fuel surcharge to zone-adjusted fare
        BigDecimal subtotalWithFuel = zoneAdjustedFare
                .add(fuelSurchargeAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return ZoneAndFuelPricingResult.builder()
                .originZone(originZone)
                .destinationZone(destinationZone)
                .zoneMultiplier(zoneMultiplier)
                .zoneAdjustedFare(zoneAdjustedFare)
                .fuelSurchargeRatePercentage(activeFuelIndex.getRatePercentage())
                .fuelSurchargeAmount(fuelSurchargeAmount)
                .subtotalWithFuel(subtotalWithFuel)
                .build();
    }
}
