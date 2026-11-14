package com.swiftdrop.parcel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelParcelRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}

