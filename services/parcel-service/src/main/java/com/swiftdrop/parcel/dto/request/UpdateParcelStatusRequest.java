package com.swiftdrop.parcel.dto.request;

import com.swiftdrop.parcel.enums.ParcelStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParcelStatusRequest {

    @NotNull(message = "New status is required")
    private ParcelStatus newStatus;

    @Size(max = 200, message = "Location description must not exceed 200 characters")
    private String location;

    private String notes;

    private String failureReason;

    private UUID assignedCourierId;
}

