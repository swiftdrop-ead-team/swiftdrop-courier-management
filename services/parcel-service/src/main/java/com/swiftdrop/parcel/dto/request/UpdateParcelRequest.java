package com.swiftdrop.parcel.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParcelRequest {

    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    private String recipientName;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,25}$", message = "Invalid recipient phone number format")
    private String recipientPhone;

    @Email(message = "Invalid recipient email address")
    @Size(max = 150, message = "Recipient email must not exceed 150 characters")
    private String recipientEmail;

    private String deliveryAddress;

    @Size(max = 100, message = "Delivery city must not exceed 100 characters")
    private String deliveryCity;

    @Size(max = 20, message = "Delivery postal code must not exceed 20 characters")
    private String deliveryPostalCode;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String specialInstructions;
}

