package com.swiftdrop.aiassistant;

import com.swiftdrop.aiassistant.service.ReportService;
import com.swiftdrop.aiassistant.service.AiAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class ReportingIntegrationTest {
    @Autowired ReportService reports;
    @Autowired AiAssistantService assistant;
    @Test void consumesActualEventShapesAndIgnoresDuplicateEventIds() {
        String parcel = UUID.randomUUID().toString(), created = UUID.randomUUID().toString();
        var booking = Map.<String,Object>of("eventId",created,"parcelId",parcel,"status","CREATED","timestamp","2030-01-02T20:00:00Z");
        reports.receive(booking); reports.receive(booking);
        reports.receive(Map.of("eventId",UUID.randomUUID().toString(),"parcelId",parcel,"newStatus","DELIVERED","timestamp","2030-01-03T01:31:00"));
        var row = reports.getAllDailyReports().stream().filter(r -> r.getReportDate().toString().equals("2030-01-03")).findFirst().orElseThrow();
        assertThat(row.getTotalDeliveries()).isEqualTo(1);
        assertThat(row.getSuccessfulDeliveries()).isEqualTo(1);
        assertThat(row.getSuccessRate()).isEqualByComparingTo("100.00");
    }
    @Test void acceptsWireTimestampArraysAndEpochSeconds() {
        String parcel = UUID.randomUUID().toString();
        reports.receive(Map.of("eventId",UUID.randomUUID().toString(),"parcelId",parcel,"status","CREATED","timestamp",new java.math.BigDecimal("1893542400.123")));
        reports.receive(Map.of("eventId",UUID.randomUUID().toString(),"parcelId",parcel,"newStatus","DELIVERED","timestamp",List.of(2030,1,3,1,31,0,123)));
        assertThat(reports.getAllDailyReports().stream().mapToInt(r -> r.getTotalDeliveries()).sum()).isEqualTo(1);
        assertThat(reports.getAllDailyReports().stream().mapToInt(r -> r.getSuccessfulDeliveries()).sum()).isEqualTo(1);
    }
    @Test void rejectsMalformedDatesWithoutRequeueLoop() {
        assertThatThrownBy(() -> reports.receive(Map.of("timestamp","invalid")))
            .isInstanceOf(org.springframework.amqp.AmqpRejectAndDontRequeueException.class);
    }
    @Test void rejectsMissingProviderCredentialsInsteadOfReturningSuccessfulAnswer() {
        assertThatThrownBy(() -> assistant.processQuery("Delivery status?", "test@example.com", List.of("CUSTOMER")))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("503");
    }
}
