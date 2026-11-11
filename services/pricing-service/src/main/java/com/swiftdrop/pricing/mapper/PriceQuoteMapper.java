package com.swiftdrop.pricing.mapper;

import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.PriceQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Maps {@link PriceQuote} entity to {@link PricingResponseDto}.
 * Responsible solely for structural field-to-field mapping.
 * Normalizes all monetary output fields to scale 2, RoundingMode.HALF_UP.
 * Does not perform any pricing calculations.
 */
@Component
public class PriceQuoteMapper {

    /**
     * Maps a {@link PriceQuote} to a {@link PricingResponseDto}.
     *
     * @param quote the persisted price quote entity (must not be null)
     * @return the populated response DTO
     */
    public PricingResponseDto toDto(PriceQuote quote) {
        if (quote == null) {
            throw new IllegalArgumentException("PriceQuote cannot be null");
        }
        return PricingResponseDto.builder()
                .id(quote.getId())
                .serviceType(quote.getServiceType())
                .originZone(quote.getOriginZone())
                .destinationZone(quote.getDestinationZone())
                .physicalWeightKg(scale2(quote.getPhysicalWeightKg()))
                .volumetricWeightKg(scale2(quote.getVolumetricWeightKg()))
                .billableWeightKg(scale2(quote.getBillableWeightKg()))
                .distanceKm(scale2(quote.getDistanceKm()))
                .baseFare(scale2(quote.getBaseFare()))
                .weightCharge(scale2(quote.getWeightCharge()))
                .distanceCharge(scale2(quote.getDistanceCharge()))
                .urgencyMultiplier(quote.getUrgencyMultiplier())
                .zoneMultiplier(quote.getZoneMultiplier())
                .fuelSurchargeAmount(scale2(quote.getFuelSurchargeAmount()))
                .merchantDiscountAmount(scale2(quote.getMerchantDiscountAmount()))
                .promoDiscountAmount(scale2(quote.getPromoDiscountAmount()))
                .finalFare(scale2(quote.getFinalFare()))
                .currency(quote.getCurrency())
                .expiresAt(quote.getExpiresAt())
                .createdAt(quote.getCreatedAt())
                .updatedAt(null) // quotes are immutable
                .build();
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
