package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for registering a new courier profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierCreateRequest {

    // Identity Service user account ID
    @NotBlank(message = "User ID is required")
    private String userId;

    @jakarta.validation.constraints.Email
    private String email;

    // Full name of the courier
    @NotBlank(message = "Full name is required")
    private String fullName;

    // Contact telephone number
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // Delivery vehicle type (MOTORBIKE, THREE_WHEELER, VAN, TRUCK)
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    // Vehicle license plate number
    private String licensePlate;

    // Preferred geographic zone for assignments
    private String assignedZone;

    // Maximum concurrent delivery capacity (must be positive)
    @Positive(message = "Max active deliveries must be positive")
    private Integer maxActiveDeliveries;
}
