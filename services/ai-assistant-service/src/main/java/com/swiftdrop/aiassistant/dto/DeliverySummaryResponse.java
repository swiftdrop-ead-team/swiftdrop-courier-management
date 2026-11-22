package com.swiftdrop.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySummaryResponse {
    private long totalDeliveries;
    private long successfulDeliveries;
    private long failedDeliveries;
    private BigDecimal overallSuccessRate;
    private BigDecimal overallFailureRate;
    private int daysTracked;
    private Instant generatedAt;
}