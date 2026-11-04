package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierResponse {
    private Long id;
    private String userId;
    private String fullName;
    private String phoneNumber;
    private VehicleType vehicleType;
    private String licensePlate;
    private String assignedZone;
    private CourierStatus status;
    private Integer maxActiveDeliveries;
    private Integer currentActiveDeliveries;
    private Boolean availableForAssignment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

