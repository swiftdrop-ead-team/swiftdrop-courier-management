package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.exception.InactiveZoneException;
import com.swiftdrop.pricing.exception.ZoneNotFoundException;
import com.swiftdrop.pricing.repository.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service managing delivery zone lookup, active validation, and zone multiplier resolution.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryZoneService {

    private final DeliveryZoneRepository deliveryZoneRepository;

    /**
     * Retrieves and validates an active delivery zone by its zone code.
     * Distinguishes between nonexistent zones and existing but inactive zones.
     *
     * @param zoneCode the unique zone code
     * @return the active {@link DeliveryZone}
     * @throws ZoneNotFoundException if the zone code does not exist in the database
     * @throws InactiveZoneException if the zone exists but is marked inactive
     */
    public DeliveryZone validateAndGetZone(String zoneCode) {
        if (zoneCode == null || zoneCode.trim().isEmpty()) {
            throw new ZoneNotFoundException("Zone code cannot be null or empty");
        }

        DeliveryZone zone = deliveryZoneRepository.findByZoneCodeIgnoreCase(zoneCode.trim())
                .orElseThrow(() -> new ZoneNotFoundException("Delivery zone not found with code: " + zoneCode));

        if (Boolean.FALSE.equals(zone.getIsActive())) {
            throw new InactiveZoneException("Delivery zone is inactive: " + zoneCode);
        }

        return zone;
    }

    /**
     * Resolves the effective zone multiplier for a shipment between origin and destination zones.
     * Per approved business rule (Option 1), the destination zone multiplier governs regional difficulty.
     *
     * @param originZone      the validated origin delivery zone
     * @param destinationZone the validated destination delivery zone
     * @return the destination zone multiplier
     */
    public BigDecimal resolveZoneMultiplier(DeliveryZone originZone, DeliveryZone destinationZone) {
        if (destinationZone == null) {
            throw new IllegalArgumentException("Destination zone cannot be null");
        }
        return destinationZone.getBaseZoneMultiplier();
    }

    /**
     * Applies the resolved zone multiplier to the strategy subtotal fare.
     *
     * @param subtotalFare   the delivery tier subtotal from the pricing strategy (urgency multiplier applied once)
     * @param zoneMultiplier the destination zone multiplier
     * @return the zone-adjusted delivery fare rounded to 2 decimal places with HALF_UP
     */
    public BigDecimal applyZoneMultiplier(BigDecimal subtotalFare, BigDecimal zoneMultiplier) {
        if (subtotalFare == null || zoneMultiplier == null) {
            throw new IllegalArgumentException("Subtotal fare and zone multiplier cannot be null");
        }
        return subtotalFare.multiply(zoneMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
