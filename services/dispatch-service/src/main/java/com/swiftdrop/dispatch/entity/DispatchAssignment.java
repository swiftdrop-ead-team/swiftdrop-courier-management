package com.swiftdrop.dispatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parcel_id", nullable = false, length = 100)
    private String parcelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @Column(name = "tracking_number", nullable = false, length = 50)
    private String trackingNumber;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProofOfDelivery proofOfDelivery;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

