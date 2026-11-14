package com.swiftdrop.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent by Dispatcher when assigning a parcel to a courier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCourierRequest {
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max=150)
    private String notificationEmail;
    @jakarta.validation.constraints.Size(max=150)
    private String notificationName;

    // ID of the parcel to assign (from Parcel Service)
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;

    // Database ID of the courier receiving the delivery job
    @NotNull(message = "Courier ID is required")
    private Long courierId;

    // Public tracking number of the parcel
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    // Sender or hub address where package is picked up
    private String pickupAddress;

    // Recipient destination address
    private String deliveryAddress;

    // Name of customer receiving the parcel
    private String recipientName;

    // Phone contact of the recipient
    private String recipientPhone;

    // Special instructions or dispatcher remarks
    private String notes;
}
