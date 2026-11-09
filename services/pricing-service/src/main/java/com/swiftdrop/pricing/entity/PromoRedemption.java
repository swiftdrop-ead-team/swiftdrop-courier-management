package com.swiftdrop.pricing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an immutable record of promo code redemption for a completed order.
 * Ensures idempotency and prevents duplicate redemption via a unique constraint on order_reference.
 * Maintains an optional foreign key reference to the PriceQuote generated at booking.
 * Mapped to the "promo_redemptions" table in PostgreSQL.
 */
@Entity
@Table(name = "promo_redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    @NotBlank(message = "Promo code snapshot is required")
    @Column(name = "promo_code", nullable = false, length = 50)
    private String promoCodeSnapshot;

    @NotBlank(message = "Order reference is required")
    @Column(name = "order_reference", nullable = false, unique = true, length = 100)
    private String orderReference;

    @Column(name = "quote_id")
    private UUID quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_promo_redemptions_quote"))
    private PriceQuote priceQuote;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @NotNull(message = "Discount amount is required")
    @DecimalMin(value = "0.00", message = "Discount amount cannot be negative")
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.01", message = "Order amount must be positive")
    @Column(name = "order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime redeemedAt = LocalDateTime.now();
}
