package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity defining baseline pricing parameters and rate metrics for a delivery urgency tier.
 * Mapped to the "pricing_rules" table in PostgreSQL.
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Service type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, unique = true, length = 30)
    private ServiceType serviceType;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.00", message = "Base fare cannot be negative")
    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @NotNull(message = "Base weight is required")
    @DecimalMin(value = "0.01", message = "Base weight must be greater than zero")
    @Column(name = "base_weight_kg", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal baseWeightKg = new BigDecimal("1.00");

    @NotNull(message = "Per kg rate is required")
    @DecimalMin(value = "0.00", message = "Per kg rate cannot be negative")
    @Column(name = "per_kg_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal perKgRate;

    @NotNull(message = "Per km rate is required")
    @DecimalMin(value = "0.00", message = "Per km rate cannot be negative")
    @Column(name = "per_km_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal perKmRate;

    @NotNull(message = "Volumetric divisor is required")
    @Min(value = 1, message = "Volumetric divisor must be positive")
    @Column(name = "volumetric_divisor", nullable = false)
    @Builder.Default
    private Integer volumetricDivisor = 5000;

    @NotNull(message = "Urgency multiplier is required")
    @DecimalMin(value = "1.0000", message = "Urgency multiplier must be at least 1.0000")
    @Column(name = "urgency_multiplier", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal urgencyMultiplier = new BigDecimal("1.0000");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
