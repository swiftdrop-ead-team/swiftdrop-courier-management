package com.swiftdrop.parcel.service.impl;

import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.service.PricingClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
public class FallbackPricingClientService implements PricingClientService {

    private final RestTemplate restTemplate;

    @Value("${swiftdrop.pricing-service.url:http://localhost:8085}")
    private String pricingServiceUrl;

    public FallbackPricingClientService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public BigDecimal calculateDeliveryFee(
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            DeliveryType deliveryType,
            boolean isFragile) {

        try {
            // Attempt remote call to Member 6's Pricing Service
            String endpoint = pricingServiceUrl + "/api/v1/pricing/calculate";
            Map<String, Object> request = Map.of(
                    "weightKg", weightKg,
                    "lengthCm", lengthCm,
                    "widthCm", widthCm,
                    "heightCm", heightCm,
                    "deliveryType", deliveryType.name(),
                    "isFragile", isFragile
            );

            var headers = new org.springframework.http.HttpHeaders();
            var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
                String authorization = servlet.getRequest().getHeader("Authorization");
                if (authorization != null) headers.set("Authorization", authorization);
            }
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object feeObj = response.getBody().get("totalFee");
                if (feeObj != null) {
                    return new BigDecimal(feeObj.toString()).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (RestClientException ex) {
            log.info("Pricing Service (Port 8085) unreachable ({}) — using deterministic fallback pricing model.", ex.getMessage());
        }

        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Pricing is temporarily unavailable. Please try again.");
    }

    /**
     * Standard IATA logistics volumetric calculation:
     * Volumetric Weight (kg) = (Length x Width x Height) / 5000
     * Chargeable Weight = max(Actual Weight, Volumetric Weight)
     */
    public BigDecimal computeLocalFallbackRate(
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            DeliveryType deliveryType,
            boolean isFragile) {

        BigDecimal actualWeight = weightKg != null ? weightKg : BigDecimal.valueOf(1.0);
        BigDecimal length = lengthCm != null ? lengthCm : BigDecimal.valueOf(10.0);
        BigDecimal width = widthCm != null ? widthCm : BigDecimal.valueOf(10.0);
        BigDecimal height = heightCm != null ? heightCm : BigDecimal.valueOf(10.0);

        BigDecimal volume = length.multiply(width).multiply(height);
        BigDecimal volumetricWeight = volume.divide(BigDecimal.valueOf(5000), 2, RoundingMode.HALF_UP);

        BigDecimal chargeableWeight = actualWeight.max(volumetricWeight);

        // Base rate: $5.00
        BigDecimal baseRate = BigDecimal.valueOf(5.00);
        // Weight rate: $2.50 per kg
        BigDecimal weightCharge = chargeableWeight.multiply(BigDecimal.valueOf(2.50));
        BigDecimal subtotal = baseRate.add(weightCharge);

        // Urgency tier multiplier
        BigDecimal multiplier = switch (deliveryType != null ? deliveryType : DeliveryType.STANDARD) {
            case STANDARD -> BigDecimal.valueOf(1.00);
            case EXPRESS -> BigDecimal.valueOf(1.50);
            case SAME_DAY -> BigDecimal.valueOf(2.20);
        };

        BigDecimal total = subtotal.multiply(multiplier);

        // Fragile item handling fee
        if (isFragile) {
            total = total.add(BigDecimal.valueOf(3.00));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}

