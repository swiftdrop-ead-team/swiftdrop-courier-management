package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.dto.PricingRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Context object encapsulating all parameters required for delivery tier pricing calculation.
 */
@Getter
@AllArgsConstructor
@Builder
public class PricingContext {

    private final BigDecimal physicalWeightKg;
    private final BigDecimal lengthCm;
    private final BigDecimal widthCm;
    private final BigDecimal heightCm;
    private final BigDecimal distanceKm;
    private final String originZone;
    private final String destinationZone;
    private final LocalDateTime requestTime;

    @Builder.Default
    private final BigDecimal surgeMultiplier = BigDecimal.ONE;

    /**
     * Constructs a PricingContext directly from a PricingRequestDto with default request time and no surge.
     */
    public static PricingContext from(PricingRequestDto dto) {
        return from(dto, null, BigDecimal.ONE);
    }

    /**
     * Constructs a PricingContext from a PricingRequestDto with a custom request time.
     */
    public static PricingContext from(PricingRequestDto dto, LocalDateTime requestTime) {
        return from(dto, requestTime, BigDecimal.ONE);
    }

    /**
     * Constructs a PricingContext from a PricingRequestDto with custom request time and surge multiplier.
     */
    public static PricingContext from(PricingRequestDto dto, LocalDateTime requestTime, BigDecimal surgeMultiplier) {
        if (dto == null) {
            throw new IllegalArgumentException("PricingRequestDto cannot be null");
        }
        return PricingContext.builder()
                .physicalWeightKg(dto.getPhysicalWeightKg())
                .lengthCm(dto.getLengthCm())
                .widthCm(dto.getWidthCm())
                .heightCm(dto.getHeightCm())
                .distanceKm(dto.getDistanceKm())
                .originZone(dto.getOriginZone())
                .destinationZone(dto.getDestinationZone())
                .requestTime(requestTime)
                .surgeMultiplier(surgeMultiplier != null ? surgeMultiplier : BigDecimal.ONE)
                .build();
    }
}
