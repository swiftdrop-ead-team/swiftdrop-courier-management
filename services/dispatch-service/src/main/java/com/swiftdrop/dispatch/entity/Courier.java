package com.swiftdrop.dispatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(name = "license_plate", length = 30)
    private String licensePlate;

    @Column(name = "assigned_zone", length = 50)
    @Builder.Default
    private String assignedZone = "COLOMBO_CENTRAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CourierStatus status = CourierStatus.AVAILABLE;

    @Column(name = "max_active_deliveries", nullable = false)
    @Builder.Default
    private Integer maxActiveDeliveries = 5;

    @Column(name = "current_active_deliveries", nullable = false)
    @Builder.Default
    private Integer currentActiveDeliveries = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean canAcceptMoreDeliveries() {
        return this.status == CourierStatus.AVAILABLE &&
               (this.currentActiveDeliveries < this.maxActiveDeliveries);
    }

    public void incrementDeliveries() {
        this.currentActiveDeliveries++;
        if (this.currentActiveDeliveries >= this.maxActiveDeliveries) {
            this.status = CourierStatus.ASSIGNED;
        }
    }

    public void decrementDeliveries() {
        if (this.currentActiveDeliveries > 0) {
            this.currentActiveDeliveries--;
        }
        if (this.status == CourierStatus.ASSIGNED && this.currentActiveDeliveries < this.maxActiveDeliveries) {
            this.status = CourierStatus.AVAILABLE;
        }
    }
}

