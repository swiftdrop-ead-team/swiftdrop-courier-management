package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOrderRequest {
    @jakarta.validation.constraints.Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String pickupDate;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.DecimalMax("100000.00")
    @jakarta.validation.constraints.Digits(integer = 6, fraction = 2)
    private BigDecimal codAmount;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "Pickup date must be a valid date today or later in Sri Lanka")
    public boolean isPickupDateValid() {
        if (pickupDate == null) return true;
        try {
            return !java.time.LocalDate.parse(pickupDate).isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Colombo")));
        } catch (java.time.format.DateTimeParseException ex) { return false; }
    }


    private String senderName;
    private String senderPhone;
    private String deliveryCity;
    @DecimalMin("0.01") @DecimalMax("300") private BigDecimal lengthCm;
    @DecimalMin("0.01") @DecimalMax("300") private BigDecimal widthCm;
    @DecimalMin("0.01") @DecimalMax("300") private BigDecimal heightCm;
    @Pattern(regexp="STANDARD|EXPRESS|SAME_DAY") private String deliveryType;
    private java.util.UUID estimateId;
    @Size(max=2000) private String specialInstructions;
    private boolean isFragile;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    private String recipientName;

    @NotBlank(message = "Recipient phone is required")
    @Pattern(regexp = "^[+0-9() .-]{7,30}$", message = "Recipient phone is invalid")
    private String recipientPhone;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 2000, message = "Delivery address must not exceed 2000 characters")
    private String deliveryAddress;

    @NotBlank(message = "Pickup address is required")
    @Size(max = 2000, message = "Pickup address must not exceed 2000 characters")
    private String pickupAddress;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Weight must be greater than zero")
    @DecimalMax(value = "100.00", message = "Weight is outside the supported range")
    @Digits(integer = 6, fraction = 2, message = "Weight supports up to two decimal places")
    private BigDecimal weightKg;
}
