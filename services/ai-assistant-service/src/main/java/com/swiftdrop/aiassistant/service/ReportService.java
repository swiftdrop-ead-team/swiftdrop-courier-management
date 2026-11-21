package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.ParcelStatusChangedEvent;
import com.swiftdrop.aiassistant.entity.DailyDeliveryStats;
import com.swiftdrop.aiassistant.entity.ProcessedEvent;
import com.swiftdrop.aiassistant.repository.DailyDeliveryStatsRepository;
import com.swiftdrop.aiassistant.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swiftdrop.aiassistant.dto.DeliverySummaryResponse;
import java.time.Instant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final DailyDeliveryStatsRepository statsRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void processParcelEvent(ParcelStatusChangedEvent event) {
        // Idempotency check: don't process the same event twice
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("Event {} already processed. Skipping.", event.getEventId());
            return;
        }

        LocalDate eventDate = event.getOccurredAt().toLocalDate();
        DailyDeliveryStats stats = statsRepository.findByReportDate(eventDate)
                .orElseGet(() -> DailyDeliveryStats.builder().reportDate(eventDate).build());

        boolean statsUpdated = false;

        if ("CREATED".equalsIgnoreCase(event.getNewStatus())) {
            stats.setTotalDeliveries(stats.getTotalDeliveries() + 1);
            statsUpdated = true;
        } else if ("DELIVERED".equalsIgnoreCase(event.getNewStatus())) {
            stats.setSuccessfulDeliveries(stats.getSuccessfulDeliveries() + 1);
            statsUpdated = true;
        } else if ("FAILED".equalsIgnoreCase(event.getNewStatus())) {
            stats.setFailedDeliveries(stats.getFailedDeliveries() + 1);
            statsUpdated = true;
        }

        if (statsUpdated) {
            recalculateRates(stats);
            statsRepository.save(stats);
        }

        // Mark event as processed
        processedEventRepository.save(ProcessedEvent.builder().eventId(event.getEventId()).build());
    }

    private void recalculateRates(DailyDeliveryStats stats) {
        if (stats.getTotalDeliveries() == 0) return;

        BigDecimal total = BigDecimal.valueOf(stats.getTotalDeliveries());
        BigDecimal success = BigDecimal.valueOf(stats.getSuccessfulDeliveries());
        BigDecimal failed = BigDecimal.valueOf(stats.getFailedDeliveries());

        stats.setSuccessRate(success.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        stats.setFailureRate(failed.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
    }

    public List<DailyDeliveryStats> getAllDailyReports() {
        return statsRepository.findAll();
    }

    public DeliverySummaryResponse getDeliverySummary() {
        List<DailyDeliveryStats> allStats = statsRepository.findAll();

        long total = allStats.stream().mapToLong(DailyDeliveryStats::getTotalDeliveries).sum();
        long success = allStats.stream().mapToLong(DailyDeliveryStats::getSuccessfulDeliveries).sum();
        long failed = allStats.stream().mapToLong(DailyDeliveryStats::getFailedDeliveries).sum();

        BigDecimal successRate = BigDecimal.ZERO;
        BigDecimal failureRate = BigDecimal.ZERO;

        if (total > 0) {
            BigDecimal totalBd = BigDecimal.valueOf(total);
            successRate = BigDecimal.valueOf(success)
                    .divide(totalBd, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            failureRate = BigDecimal.valueOf(failed)
                    .divide(totalBd, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return DeliverySummaryResponse.builder()
                .totalDeliveries(total)
                .successfulDeliveries(success)
                .failedDeliveries(failed)
                .overallSuccessRate(successRate)
                .overallFailureRate(failureRate)
                .daysTracked(allStats.size())
                .generatedAt(Instant.now())
                .build();
    }
}