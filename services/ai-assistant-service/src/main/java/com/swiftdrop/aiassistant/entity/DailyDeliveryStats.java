package com.swiftdrop.aiassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_delivery_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyDeliveryStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_deliveries", nullable = false)
    private Integer totalDeliveries = 0;

    @Column(name = "successful_deliveries", nullable = false)
    private Integer successfulDeliveries = 0;

    @Column(name = "failed_deliveries", nullable = false)
    private Integer failedDeliveries = 0;

    @Column(name = "success_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal successRate = BigDecimal.ZERO;

    @Column(name = "failure_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal failureRate = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}