package com.swiftdrop.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParcelOrderResponse(@com.fasterxml.jackson.annotation.JsonAlias("id") String parcelId, String trackingNumber) {
}
