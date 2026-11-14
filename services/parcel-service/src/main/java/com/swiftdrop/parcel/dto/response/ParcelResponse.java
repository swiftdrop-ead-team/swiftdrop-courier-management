package com.swiftdrop.parcel.dto.response;

import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponse {
    private String pickupDate;
    private BigDecimal codAmount;

    private UUID id;
    private String trackingNumber;
    private UUID senderId;
    private SenderType senderType;
    private String senderName;
    private String senderPhone;
    private String senderEmail;
    private String pickupAddress;
    private String pickupCity;
    private String pickupPostalCode;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPostalCode;
    private BigDecimal weightKg;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private String description;
    private boolean isFragile;
    private DeliveryType deliveryType;
    private ParcelStatus status;
    private BigDecimal deliveryFee;
    private String currency;
    private UUID assignedCourierId;
    private String failureReason;
    private String specialInstructions;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ParcelStatusHistoryResponse> statusHistory;
}

