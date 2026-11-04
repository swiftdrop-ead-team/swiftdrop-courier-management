package com.swiftdrop.dispatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DispatchAssignment entity representing a parcel delivery job allocated to a courier.
 * Mapped to the "dispatch_assignments" table in PostgreSQL.
 */
@Entity
@Table(name = "dispatch_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchAssignment {

    // Unique primary key ID for this dispatch assignment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign identifier referencing the parcel managed by Parcel Service
    @Column(name = "parcel_id", nullable = false, length = 100)
    private String parcelId;

    // Many assignments can belong to one courier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    // Unique public tracking code (e.g., "SWIFT-COL-1029")
    @Column(name = "tracking_number", nullable = false, length = 50)
    private String trackingNumber;

    // Physical pickup address where courier collects the package
    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    // Destination address where parcel must be handed over
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    // Name of the package recipient
    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    // Phone number of the recipient for delivery coordination
    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    // Current delivery status (ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    // Reason explanation if the delivery attempt was unsuccessful
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Operational delivery remarks or driver notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Timestamp when the dispatcher assigned this parcel
    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    // Timestamp when the delivery was finished (delivered or failed)
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // One-to-one relationship with ProofOfDelivery record
    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProofOfDelivery proofOfDelivery;

    // Audit timestamp when record was created
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Audit timestamp when record was last updated
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
