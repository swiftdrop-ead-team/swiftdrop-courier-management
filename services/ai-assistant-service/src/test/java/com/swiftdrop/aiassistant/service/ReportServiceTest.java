package com.swiftdrop.aiassistant.service;

import com.swiftdrop.aiassistant.dto.DeliverySummaryResponse;
import com.swiftdrop.aiassistant.entity.DailyDeliveryStats;
import com.swiftdrop.aiassistant.repository.DailyDeliveryStatsRepository;
import com.swiftdrop.aiassistant.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private DailyDeliveryStatsRepository statsRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getDeliverySummary_calculatesTotalsAndRatesAccurately() {
        DailyDeliveryStats day1 = DailyDeliveryStats.builder()
                .reportDate(LocalDate.of(2026, 11, 19))
                .totalDeliveries(10)
                .successfulDeliveries(8)
                .failedDeliveries(2)
                .build();

        DailyDeliveryStats day2 = DailyDeliveryStats.builder()
                .reportDate(LocalDate.of(2026, 11, 20))
                .totalDeliveries(10)
                .successfulDeliveries(7)
                .failedDeliveries(3)
                .build();

        when(statsRepository.findAll()).thenReturn(List.of(day1, day2));

        DeliverySummaryResponse summary = reportService.getDeliverySummary();

        assertEquals(20, summary.getTotalDeliveries());
        assertEquals(15, summary.getSuccessfulDeliveries());
        assertEquals(5, summary.getFailedDeliveries());
        assertEquals(2, summary.getDaysTracked());
        assertEquals(0, new BigDecimal("75.0000").compareTo(summary.getOverallSuccessRate()));
        assertEquals(0, new BigDecimal("25.0000").compareTo(summary.getOverallFailureRate()));
    }
}
