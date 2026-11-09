package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a geographic delivery zone with an associated regional cost multiplier.
 * Mapped to the "delivery_zones" table in PostgreSQL.
 */
@Entity
@Table(name = "delivery_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Zone code is required")
    @Column(name = "zone_code", nullable = false, unique = true, length = 50)
    private String zoneCode;

    @NotBlank(message = "Zone name is required")
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @NotNull(message = "Base zone multiplier is required")
    @DecimalMin(value = "0.0001", message = "Base zone multiplier must be greater than zero")
    @Column(name = "base_zone_multiplier", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal baseZoneMultiplier = new BigDecimal("1.0000");

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
