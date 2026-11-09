package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking national floating fuel surcharge percentage index.
 * Mapped to the "fuel_surcharge_index" table in PostgreSQL.
 */
@Entity
@Table(name = "fuel_surcharge_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelSurchargeIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Rate percentage is required")
    @DecimalMin(value = "0.00", message = "Rate percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Rate percentage cannot exceed 100%")
    @Column(name = "rate_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercentage;

    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_by", length = 100)
    @Builder.Default
    private String updatedBy = "SYSTEM";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
