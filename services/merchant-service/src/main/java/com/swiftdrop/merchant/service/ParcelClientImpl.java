package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.ParcelOrderRequest;
import com.swiftdrop.merchant.dto.ParcelOrderResponse;
import com.swiftdrop.merchant.exception.ParcelServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class ParcelClientImpl implements ParcelClient {

    private final RestClient restClient;
    private final String orderPath;
    private final MerchantServiceToken tokens;

    public ParcelClientImpl(RestClient restClient, String orderPath, MerchantServiceToken tokens) {
        this.restClient = restClient;
        this.orderPath = orderPath; this.tokens=tokens;
    }

    @Override
    public ParcelOrderResponse createParcel(String idempotencyKey, ParcelOrderRequest request) {
        try {
            var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if(auth==null || !(auth.getPrincipal() instanceof com.swiftdrop.merchant.dto.MerchantPrincipal principal))
                throw new ParcelServiceException("Merchant authentication is required");
            var payload = new java.util.LinkedHashMap<String,Object>();
            if(request.parcelDetails()!=null)payload.putAll(request.parcelDetails());
            payload.put("merchantOrderReference",idempotencyKey);
            payload.put("recipientName",request.recipientName());payload.put("recipientPhone",request.recipientPhone());
            payload.put("deliveryAddress",request.deliveryAddress());payload.put("pickupAddress",request.pickupAddress());
            payload.put("description",request.description());payload.put("weightKg",request.weightKg());
            payload.values().removeIf(java.util.Objects::isNull);
            ParcelOrderResponse response = restClient.post()
                    .uri(orderPath)
                    .header("Idempotency-Key", idempotencyKey)
                    .header("Authorization","Bearer "+tokens.forMerchant(principal))
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        if(httpResponse.getStatusCode().is4xxClientError()) throw new org.springframework.web.server.ResponseStatusException(httpResponse.getStatusCode(),"Parcel rejected the booking. Check the details or request a new estimate.");
                        throw new ParcelServiceException("Parcel Service returned HTTP "
                                + httpResponse.getStatusCode().value());
                    })
                    .body(ParcelOrderResponse.class);
            if (response == null || response.parcelId() == null || response.parcelId().isBlank()) {
                throw new ParcelServiceException("Parcel Service returned an incomplete order response");
            }
            return response;
        } catch (ParcelServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ParcelServiceException("Parcel Service request failed", ex);
        }
    }
}
