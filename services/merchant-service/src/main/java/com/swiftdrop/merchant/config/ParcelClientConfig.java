package com.swiftdrop.merchant.config;

import com.swiftdrop.merchant.service.ParcelClient;
import com.swiftdrop.merchant.service.ParcelClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ParcelClientConfig {

    @Bean
    ParcelClient parcelClient(RestClient.Builder builder,
                              @Value("${merchant.parcel.base-url}") String baseUrl,
                              @Value("${merchant.parcel.order-path}") String orderPath) {
        return new ParcelClientImpl(builder.baseUrl(baseUrl).build(), orderPath);
    }
}
