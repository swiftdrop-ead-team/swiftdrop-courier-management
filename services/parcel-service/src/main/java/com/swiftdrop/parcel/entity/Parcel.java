package com.swiftdrop.parcel.entity;

import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "parcels", indexes = {
    @Index(name = "idx_parcels_tracking_number", columnList = "tracking_number"),
    @Index(name = "idx_parcels_sender_id", columnList = "sender_id"),
    @Index(name = "idx_parcels_status", columnList = "status"),
    @Index(name = "idx_parcels_assigned_courier_id", columnList = "assigned_courier_id"),
    @Index(name = "idx_parcels_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 30)
    private String trackingNumber;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    @Builder.Default
    private SenderType senderType = SenderType.CUSTOMER;

    @Column(name = "sender_name", nullable = false, length = 150)
    private String senderName;

    @Column(name = "sender_phone", nullable = false, length = 30)
    private String senderPhone;

    @Column(name = "sender_email", length = 150)
    private String senderEmail;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_city", nullable = false, length = 100)
    private String pickupCity;

    @Column(name = "pickup_postal_code", nullable = false, length = 20)
    private String pickupPostalCode;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_city", nullable = false, length = 100)
    private String deliveryCity;

    @Column(name = "delivery_postal_code", nullable = false, length = 20)
    private String deliveryPostalCode;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "length_cm", nullable = false, precision = 6, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", nullable = false, precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_fragile", nullable = false)
    @Builder.Default
    private boolean isFragile = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 30)
    @Builder.Default
    private DeliveryType deliveryType = DeliveryType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ParcelStatus status = ParcelStatus.CREATED;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "assigned_courier_id")
    private UUID assignedCourierId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<ParcelStatusHistory> statusHistory = new ArrayList<>();

    public void addStatusHistory(ParcelStatusHistory history) {
        statusHistory.add(history);
        history.setParcel(this);
    }
}

