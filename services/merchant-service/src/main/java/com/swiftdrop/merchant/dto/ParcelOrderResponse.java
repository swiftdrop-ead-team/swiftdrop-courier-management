package com.swiftdrop.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParcelOrderResponse(String parcelId, String trackingNumber) {
}
