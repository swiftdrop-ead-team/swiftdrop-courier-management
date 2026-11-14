package com.swiftdrop.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_merchant_order_idempotency",
                columnNames = {"merchant_id", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MerchantOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "parcel_id", unique = true, length = 100)
    private String parcelId;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(length = 500)
    private String description;

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "delivery_status", nullable = false, length = 50)
    @Builder.Default
    private String deliveryStatus = "CREATED";

    @Column(name = "parcel_status_updated_at")
    private LocalDateTime parcelStatusUpdatedAt;

    @Column(name = "status_version")
    private Long statusVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
