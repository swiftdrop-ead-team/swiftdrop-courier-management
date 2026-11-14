package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.ParcelOrderRequest;
import com.swiftdrop.merchant.dto.ParcelOrderResponse;

public interface ParcelClient {

    ParcelOrderResponse createParcel(String idempotencyKey, ParcelOrderRequest request);
}
