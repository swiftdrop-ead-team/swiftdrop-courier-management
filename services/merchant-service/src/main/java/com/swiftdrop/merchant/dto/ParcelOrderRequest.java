package com.swiftdrop.merchant.dto;

import java.math.BigDecimal;

public record ParcelOrderRequest(
        String merchantOrderReference,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        String pickupAddress,
        String description,
        BigDecimal weightKg,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Map<String,Object> parcelDetails) {
    public ParcelOrderRequest(String reference,String name,String phone,String delivery,String pickup,String description,BigDecimal weight) {
        this(reference,name,phone,delivery,pickup,description,weight,null);
    }
}
