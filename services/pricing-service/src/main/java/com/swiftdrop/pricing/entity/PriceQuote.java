package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an immutable price quote generated for parcel booking.
 * Holds guaranteed pricing parameters for a specified validity period before checkout.
 * Mapped to the "price_quotes" table in PostgreSQL.
 */
@Entity
@Table(name = "price_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Service type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private ServiceType serviceType;

    @NotBlank(message = "Origin zone is required")
    @Column(name = "origin_zone", nullable = false, length = 50)
    private String originZone;

    @NotBlank(message = "Destination zone is required")
    @Column(name = "destination_zone", nullable = false, length = 50)
    private String destinationZone;

    @NotNull(message = "Physical weight is required")
    @DecimalMin(value = "0.01", message = "Physical weight must be greater than zero")
    @Column(name = "physical_weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal physicalWeightKg;

    @NotNull(message = "Volumetric weight is required")
    @DecimalMin(value = "0.00", message = "Volumetric weight cannot be negative")
    @Column(name = "volumetric_weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal volumetricWeightKg;

    @NotNull(message = "Billable weight is required")
    @DecimalMin(value = "0.01", message = "Billable weight must be greater than zero")
    @Column(name = "billable_weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal billableWeightKg;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.00", message = "Distance cannot be negative")
    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @NotNull(message = "Base fare is required")
    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @NotNull(message = "Weight charge is required")
    @Column(name = "weight_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightCharge;

    @NotNull(message = "Distance charge is required")
    @Column(name = "distance_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceCharge;

    @NotNull(message = "Urgency multiplier is required")
    @Column(name = "urgency_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal urgencyMultiplier;

    @NotNull(message = "Zone multiplier is required")
    @Column(name = "zone_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal zoneMultiplier;

    @NotNull(message = "Fuel surcharge amount is required")
    @Column(name = "fuel_surcharge_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fuelSurchargeAmount = BigDecimal.ZERO;

    @NotNull(message = "Merchant discount amount is required")
    @Column(name = "merchant_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal merchantDiscountAmount = BigDecimal.ZERO;

    @NotNull(message = "Promo discount amount is required")
    @Column(name = "promo_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal promoDiscountAmount = BigDecimal.ZERO;

    @NotNull(message = "Final fare is required")
    @Column(name = "final_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalFare;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "LKR";

    @NotNull(message = "Quote expiration timestamp is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
