package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for changing courier operational status (e.g., to OFFLINE or AVAILABLE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierStatusUpdateRequest {

    // The new operational status to apply
    @NotNull(message = "Courier status is required")
    private CourierStatus status;
}
