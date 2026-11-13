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

    public ParcelClientImpl(RestClient restClient, String orderPath) {
        this.restClient = restClient;
        this.orderPath = orderPath;
    }

    @Override
    public ParcelOrderResponse createParcel(String idempotencyKey, ParcelOrderRequest request) {
        try {
            ParcelOrderResponse response = restClient.post()
                    .uri(orderPath)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
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
