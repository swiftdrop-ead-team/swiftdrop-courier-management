package com.swiftdrop.dispatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Courier entity representing a delivery rider or driver.
 * Mapped to the "couriers" table in PostgreSQL (swiftdrop_dispatch database).
 */
@Entity
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    // Unique primary key ID generated automatically by PostgreSQL
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // Foreign reference ID linking to the user account in Identity Service
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @jakarta.validation.constraints.Email
    private String email;

    // Courier's full name (e.g., "Kasun Perera")
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    // Contact telephone number (e.g., "+94771234567")
    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    // Vehicle category used for deliveries (MOTORBIKE, VAN, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    // Vehicle registration number plate (e.g., "WP BDF-1029")
    @Column(name = "license_plate", length = 30)
    private String licensePlate;

    // Geographical zone assigned to the courier (e.g., "COLOMBO_CENTRAL", "KANDY")
    @Column(name = "assigned_zone", length = 50)
    @Builder.Default
    private String assignedZone = "COLOMBO_CENTRAL";

    // Current availability status (AVAILABLE, ASSIGNED, DELIVERING, OFFLINE)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CourierStatus status = CourierStatus.AVAILABLE;

    // Maximum number of active deliveries this courier is allowed to hold simultaneously
    @Column(name = "max_active_deliveries", nullable = false)
    @Builder.Default
    private Integer maxActiveDeliveries = 5;

    // Number of deliveries currently in-progress for this courier
    @Column(name = "current_active_deliveries", nullable = false)
    @Builder.Default
    private Integer currentActiveDeliveries = 0;

    // Timestamp when this courier record was created in the database
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when this courier record was last modified
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if the courier can accept an additional delivery.
     * Must be AVAILABLE and current active deliveries must be less than max limit.
     */
    public boolean canAcceptMoreDeliveries() {
        return this.status == CourierStatus.AVAILABLE &&
               (this.currentActiveDeliveries < this.maxActiveDeliveries);
    }

    /**
     * Increments the count of active deliveries by 1 when a parcel is assigned.
     * If current load reaches max limit, updates status to ASSIGNED (busy).
     */
    public void incrementDeliveries() {
        this.currentActiveDeliveries++;
        if (this.currentActiveDeliveries >= this.maxActiveDeliveries) {
            this.status = CourierStatus.ASSIGNED;
        }
    }

    /**
     * Decrements the active delivery count by 1 when a delivery completes or fails.
     * Restores status to AVAILABLE if below max capacity.
     */
    public void decrementDeliveries() {
        if (this.currentActiveDeliveries > 0) {
            this.currentActiveDeliveries--;
        }
        if (this.status == CourierStatus.ASSIGNED && this.currentActiveDeliveries < this.maxActiveDeliveries) {
            this.status = CourierStatus.AVAILABLE;
        }
    }
}
