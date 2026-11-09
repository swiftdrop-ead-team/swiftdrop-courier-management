package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing promotional discount coupons.
 * Mapped to the "promo_codes" table in PostgreSQL.
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Promo code is required")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "Discount type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be positive")
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @NotNull(message = "Minimum order amount is required")
    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.01", message = "Max discount cap must be positive")
    @Column(name = "max_discount_cap", precision = 12, scale = 2)
    private BigDecimal maxDiscountCap;

    @NotNull(message = "Validity start time is required")
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @NotNull(message = "Validity end time is required")
    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Min(value = 1, message = "Usage limit must be at least 1")
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Min(value = 0, message = "Times used cannot be negative")
    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "promoCode", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromoRedemption> redemptions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
