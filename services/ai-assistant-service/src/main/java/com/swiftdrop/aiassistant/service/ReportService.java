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

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = com.swiftdrop.aiassistant.config.RabbitMQConfig.REPORTS_QUEUE)
    @Transactional
    public void receive(java.util.Map<String, Object> payload) {
        var event = new ParcelStatusChangedEvent();
        try {
            event.setEventId((String) payload.get("eventId"));
            event.setParcelId((String) payload.get("parcelId"));
            event.setNewStatus((String) payload.getOrDefault("newStatus", payload.get("status")));
            Object raw = payload.getOrDefault("occurredAt", payload.get("timestamp"));
            if (raw instanceof java.util.List<?> parts) {
                int[] values = parts.stream().mapToInt(part -> ((Number) part).intValue()).toArray();
                event.setOccurredAt(java.time.LocalDateTime.of(values[0], values[1], values[2], values[3], values[4],
                    values.length > 5 ? values[5] : 0, values.length > 6 ? values[6] : 0));
            } else if (raw instanceof Number number) {
                var epoch = new java.math.BigDecimal(number.toString());
                var instant = java.time.Instant.ofEpochSecond(epoch.longValue(), epoch.remainder(java.math.BigDecimal.ONE).movePointRight(9).longValue());
                event.setOccurredAt(instant.atZone(java.time.ZoneId.of("Asia/Colombo")).toLocalDateTime());
            } else {
                String timestamp = String.valueOf(raw);
                try { event.setOccurredAt(java.time.Instant.parse(timestamp).atZone(java.time.ZoneId.of("Asia/Colombo")).toLocalDateTime()); }
                catch (java.time.format.DateTimeParseException local) { event.setOccurredAt(java.time.LocalDateTime.parse(timestamp)); }
            }
            if (event.getEventId() == null || event.getParcelId() == null || event.getNewStatus() == null)
                throw new IllegalArgumentException("Incomplete reporting event");
        } catch (RuntimeException invalid) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Invalid reporting event", invalid);
        }
        processParcelEvent(event);
    }

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
}