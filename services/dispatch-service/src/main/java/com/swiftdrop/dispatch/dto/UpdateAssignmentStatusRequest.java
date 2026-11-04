package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent by couriers or system when updating delivery status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentStatusRequest {

    // Target lifecycle status (PICKED_UP, IN_TRANSIT, DELIVERED, FAILED, etc.)
    @NotNull(message = "Assignment status is required")
    private AssignmentStatus status;

    // Compulsory explanation if status is marked FAILED (e.g., "Customer phone switched off")
    private String failureReason;

    // Optional status update notes or driver remarks
    private String notes;
}
