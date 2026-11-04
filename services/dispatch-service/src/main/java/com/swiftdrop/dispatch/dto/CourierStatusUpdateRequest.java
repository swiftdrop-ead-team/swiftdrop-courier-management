package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.entity.CourierStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierStatusUpdateRequest {

    @NotNull(message = "Courier status is required")
    private CourierStatus status;
}

