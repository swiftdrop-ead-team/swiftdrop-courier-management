package com.swiftdrop.parcel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.enums.DeliveryType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Short-lived, owner-bound estimates using the same calculator as parcel creation. */
@Service
public class ParcelEstimateService {
    public record Estimate(UUID id, BigDecimal amount, String currency, Instant expiresAt) {}
    private final com.swiftdrop.parcel.repository.ParcelEstimateRepository estimates;
    private final PricingClientService pricing;
    private final ObjectMapper mapper;

    public ParcelEstimateService(PricingClientService pricing, ObjectMapper mapper, com.swiftdrop.parcel.repository.ParcelEstimateRepository estimates) {
        this.pricing = pricing; this.mapper = mapper; this.estimates = estimates;
    }
    @org.springframework.transaction.annotation.Transactional
    public Estimate estimate(CreateParcelRequest request, UUID owner) {
        BigDecimal amount = pricing.calculateDeliveryFee(value(request.getWeightKg(), "1"), value(request.getLengthCm(), "10"), value(request.getWidthCm(), "10"), value(request.getHeightCm(), "10"), request.getDeliveryType() == null ? DeliveryType.STANDARD : request.getDeliveryType(), request.isFragile());
        Estimate estimate = new Estimate(UUID.randomUUID(), amount, "LKR", Instant.now().plusSeconds(300));
        var row = new com.swiftdrop.parcel.entity.ParcelEstimate();
        row.setId(estimate.id());row.setOwnerId(owner);row.setPayload(fingerprint(request));
        row.setAmount(amount);row.setCurrency(estimate.currency());row.setExpiresAt(estimate.expiresAt());estimates.save(row);
        return estimate;
    }
    @org.springframework.transaction.annotation.Transactional
    public BigDecimal consume(CreateParcelRequest request, UUID owner) {
        var row=estimates.findById(request.getEstimateId()).orElse(null);
        if(row==null || row.getConsumedAt()!=null || !row.getOwnerId().equals(owner)
            || !row.getExpiresAt().isAfter(Instant.now()) || !row.getPayload().equals(fingerprint(request))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"The estimate expired, was already used, or the booking changed. Request a new estimate.");
        }
        row.setConsumedAt(Instant.now());estimates.save(row);
        return row.getAmount();
    }

    private String fingerprint(CreateParcelRequest request) {
        ObjectNode payload = mapper.valueToTree(request); payload.remove("estimateId"); payload.remove("merchantOrderReference"); return payload.toString();
    }
    private BigDecimal value(BigDecimal value, String fallback) { return value == null ? new BigDecimal(fallback) : value; }
}
