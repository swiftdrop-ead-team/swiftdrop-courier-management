package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Detailed courier profile response returned to client callers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierResponse {

    // Database ID of the courier
    private Long id;

    // Associated Identity Service user ID
    private String userId;

    // Full name of courier
    private String fullName;

    // Telephone contact number
    private String phoneNumber;

    // Vehicle type used
    private VehicleType vehicleType;

    // Vehicle license plate number
    private String licensePlate;

    // Operating geographical region
    private String assignedZone;

    // Current operational status (AVAILABLE, ASSIGNED, DELIVERING, OFFLINE)
    private CourierStatus status;

    // Max deliveries permitted at one time
    private Integer maxActiveDeliveries;

    // Deliveries currently in-progress
    private Integer currentActiveDeliveries;

    // Convenience flag: true if courier has capacity and is available
    private Boolean availableForAssignment;

    // Creation audit timestamp
    private LocalDateTime createdAt;

    // Last modification audit timestamp
    private LocalDateTime updatedAt;
}
