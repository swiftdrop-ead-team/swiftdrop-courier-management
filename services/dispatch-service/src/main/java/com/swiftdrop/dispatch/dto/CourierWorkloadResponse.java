package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierWorkloadResponse {
    private Long courierId;
    private String courierName;
    private String phoneNumber;
    private VehicleType vehicleType;
    private String assignedZone;
    private CourierStatus status;
    private Integer activeDeliveries;
    private Integer maxDeliveries;
    private Double utilizationPercentage;
    private Boolean canAcceptMore;
}

