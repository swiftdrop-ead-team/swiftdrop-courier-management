package com.swiftdrop.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCourierRequest {

    @NotBlank(message = "Parcel ID is required")
    private String parcelId;

    @NotNull(message = "Courier ID is required")
    private Long courierId;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    private String pickupAddress;

    private String deliveryAddress;

    private String recipientName;

    private String recipientPhone;

    private String notes;
}

