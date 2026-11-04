package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentStatusRequest {

    @NotNull(message = "Assignment status is required")
    private AssignmentStatus status;

    private String failureReason;

    private String notes;
}

