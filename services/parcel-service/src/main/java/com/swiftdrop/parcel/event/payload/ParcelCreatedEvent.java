package com.swiftdrop.parcel.event.payload;

import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelCreatedEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "PARCEL_CREATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID parcelId;
    private String trackingNumber;
    private UUID senderId;
    private SenderType senderType;
    private String pickupCity;
    private String deliveryCity;
    private BigDecimal weightKg;
    private DeliveryType deliveryType;
    private ParcelStatus status;
    private BigDecimal deliveryFee;
    private String currency;
}

