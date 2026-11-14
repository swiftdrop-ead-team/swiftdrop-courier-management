package com.swiftdrop.parcel.dto.request;

import com.swiftdrop.parcel.enums.DeliveryType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateParcelRequest {

    @Size(max = 150, message = "Sender name must not exceed 150 characters")
    private String senderName;

    @Pattern(regexp = "^$|^\\+?[0-9\\s\\-()]{7,25}$", message = "Invalid sender phone number format")
    private String senderPhone;

    @Email(message = "Invalid sender email address")
    @Size(max = 150, message = "Sender email must not exceed 150 characters")
    private String senderEmail;

    private String pickupAddress;

    @Size(max = 100, message = "Pickup city must not exceed 100 characters")
    private String pickupCity;

    @Size(max = 20, message = "Pickup postal code must not exceed 20 characters")
    private String pickupPostalCode;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    private String recipientName;

    @Pattern(regexp = "^$|^\\+?[0-9\\s\\-()]{7,25}$", message = "Invalid recipient phone number format")
    private String recipientPhone;

    @Email(message = "Invalid recipient email address")
    @Size(max = 150, message = "Recipient email must not exceed 150 characters")
    private String recipientEmail;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    @Size(max = 100, message = "Delivery city must not exceed 100 characters")
    private String deliveryCity;

    @Size(max = 20, message = "Delivery postal code must not exceed 20 characters")
    private String deliveryPostalCode;

    @Positive(message = "Weight must be greater than zero")
    @DecimalMax(value = "100.00", message = "Weight cannot exceed 100 kg")
    private BigDecimal weightKg;

    @Positive(message = "Length must be greater than zero")
    @DecimalMax(value = "300.00", message = "Length cannot exceed 300 cm")
    private BigDecimal lengthCm;

    @Positive(message = "Width must be greater than zero")
    @DecimalMax(value = "300.00", message = "Width cannot exceed 300 cm")
    private BigDecimal widthCm;

    @Positive(message = "Height must be greater than zero")
    @DecimalMax(value = "300.00", message = "Height cannot exceed 300 cm")
    private BigDecimal heightCm;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Builder.Default
    private boolean isFragile = false;

    @Builder.Default
    private DeliveryType deliveryType = DeliveryType.STANDARD;

    private String specialInstructions;
}
