package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.enums.DeliveryType;

import java.math.BigDecimal;

public interface PricingClientService {

    BigDecimal calculateDeliveryFee(
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            DeliveryType deliveryType,
            boolean isFragile
    );
}

