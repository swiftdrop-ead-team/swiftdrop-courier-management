package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytical workload data response for dispatchers to monitor fleet utilization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierWorkloadResponse {

    // Database ID of courier
    private Long courierId;

    // Name of courier
    private String courierName;

    // Contact telephone
    private String phoneNumber;

    // Vehicle used
    private VehicleType vehicleType;

    // Operating region
    private String assignedZone;

    // Status of courier
    private CourierStatus status;

    // Number of parcels currently in delivery
    private Integer activeDeliveries;

    // Maximum capacity allowed
    private Integer maxDeliveries;

    // Capacity utilization as a percentage (e.g., 60.0%)
    private Double utilizationPercentage;

    // True if courier can take at least one more parcel
    private Boolean canAcceptMore;
}
