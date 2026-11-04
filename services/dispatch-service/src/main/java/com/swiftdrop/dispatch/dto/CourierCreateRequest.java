package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierCreateRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String licensePlate;

    private String assignedZone;

    @Positive(message = "Max active deliveries must be positive")
    private Integer maxActiveDeliveries;
}

