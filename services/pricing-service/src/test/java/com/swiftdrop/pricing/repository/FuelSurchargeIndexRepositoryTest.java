package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FuelSurchargeIndexRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FuelSurchargeIndexRepository fuelSurchargeIndexRepository;

    @Test
    @DisplayName("Scenario 1: Single active record with effectiveFrom in the past is returned")
    void shouldReturnActiveRecordWhenEffectiveInPast() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        FuelSurchargeIndex pastRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("4.50"))
                .effectiveFrom(now.minusDays(5))
                .isActive(true)
                .updatedBy("ADMIN-01")
                .build();
        entityManager.persistAndFlush(pastRecord);
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertTrue(result.isPresent(), "Expected an active fuel surcharge rate to be present");
        assertEquals(0, new BigDecimal("4.50").compareTo(result.get().getRatePercentage()));
        assertEquals(pastRecord.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Scenario 2: Future-dated active record is excluded from results")
    void shouldExcludeFutureDatedActiveRecord() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        FuelSurchargeIndex futureRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("6.00"))
                .effectiveFrom(now.plusDays(3))
                .isActive(true)
                .updatedBy("ADMIN-FUTURE")
                .build();
        entityManager.persistAndFlush(futureRecord);
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertFalse(result.isPresent(), "Future-dated active record must not be returned");
    }

    @Test
    @DisplayName("Scenario 3: Inactive record is excluded even if effectiveFrom is in the past")
    void shouldExcludeInactiveRecordEvenIfEffectiveInPast() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        FuelSurchargeIndex inactiveRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.25"))
                .effectiveFrom(now.minusDays(10))
                .isActive(false)
                .updatedBy("ADMIN-INACTIVE")
                .build();
        entityManager.persistAndFlush(inactiveRecord);
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertFalse(result.isPresent(), "Inactive record must not be returned");
    }

    @Test
    @DisplayName("Scenario 4: When multiple active past records exist, return the one with the latest effectiveFrom")
    void shouldReturnMostRecentRecordWhenMultipleActivePastRecordsExist() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        FuelSurchargeIndex oldest = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.00"))
                .effectiveFrom(now.minusDays(15))
                .isActive(true)
                .build();
        FuelSurchargeIndex middle = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("4.00"))
                .effectiveFrom(now.minusDays(7))
                .isActive(true)
                .build();
        FuelSurchargeIndex newestPast = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.50"))
                .effectiveFrom(now.minusDays(1))
                .isActive(true)
                .build();

        entityManager.persist(oldest);
        entityManager.persist(middle);
        entityManager.persist(newestPast);
        entityManager.flush();
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("5.50").compareTo(result.get().getRatePercentage()));
        assertEquals(newestPast.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Scenario 5: Empty repository returns empty Optional")
    void shouldReturnEmptyWhenNoRecordsExist() {
        LocalDateTime now = LocalDateTime.now();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertFalse(result.isPresent(), "Expected empty Optional when no records exist");
    }

    @Test
    @DisplayName("Scenario 6: Returns empty Optional when only future active and past inactive records exist")
    void shouldReturnEmptyWhenOnlyFutureOrInactiveRecordsExist() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        FuelSurchargeIndex pastInactive = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.50"))
                .effectiveFrom(now.minusDays(4))
                .isActive(false)
                .build();
        FuelSurchargeIndex futureActive = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("6.50"))
                .effectiveFrom(now.plusDays(2))
                .isActive(true)
                .build();

        entityManager.persist(pastInactive);
        entityManager.persist(futureActive);
        entityManager.flush();
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertFalse(result.isPresent(), "Expected empty Optional when no records satisfy both criteria");
    }

    @Test
    @DisplayName("Explicit Requirement: A record whose effectiveFrom is exactly equal to now must be returned")
    void shouldReturnRecordWhenEffectiveFromIsExactlyEqualToNow() {
        LocalDateTime exactNow = LocalDateTime.of(2026, 11, 10, 12, 0, 0);

        FuelSurchargeIndex exactMatch = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("4.85"))
                .effectiveFrom(exactNow)
                .isActive(true)
                .updatedBy("EXACT-MATCH")
                .build();
        entityManager.persistAndFlush(exactMatch);
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(exactNow);

        assertTrue(result.isPresent(), "Record with effectiveFrom == now must be returned (boundary <=)");
        assertEquals(0, new BigDecimal("4.85").compareTo(result.get().getRatePercentage()));
        assertEquals(exactMatch.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Explicit Requirement: When two records have the same effectiveFrom, return the one with later createdAt")
    void shouldReturnRecordWithLaterCreatedAtWhenEffectiveFromIsIdentical() throws InterruptedException {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime sameEffectiveFrom = now.minusDays(2);

        // Persist earlier created record
        FuelSurchargeIndex earlierCreated = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.75"))
                .effectiveFrom(sameEffectiveFrom)
                .isActive(true)
                .updatedBy("FIRST-CREATED")
                .build();
        entityManager.persistAndFlush(earlierCreated);

        // Ensure distinct createdAt timestamps across system clocks
        Thread.sleep(50);

        // Persist later created record with identical effectiveFrom
        FuelSurchargeIndex laterCreated = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.25"))
                .effectiveFrom(sameEffectiveFrom)
                .isActive(true)
                .updatedBy("SECOND-CREATED")
                .build();
        entityManager.persistAndFlush(laterCreated);

        // Verify that createdAt is strictly later for the second record
        assertNotNull(earlierCreated.getCreatedAt());
        assertNotNull(laterCreated.getCreatedAt());
        assertTrue(laterCreated.getCreatedAt().isAfter(earlierCreated.getCreatedAt()),
                "Second record's createdAt must be strictly after first record's createdAt");

        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive(now);

        assertTrue(result.isPresent(), "Expected an active record to be returned");
        assertEquals(0, new BigDecimal("5.25").compareTo(result.get().getRatePercentage()),
                "Tie-breaker must return the record with the later createdAt");
        assertEquals(laterCreated.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Convenience overload: findCurrentActive() without arguments uses LocalDateTime.now()")
    void shouldReturnCurrentActiveUsingDefaultNoArgMethod() {
        FuelSurchargeIndex currentActive = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("4.10"))
                .effectiveFrom(LocalDateTime.now().minusHours(1))
                .isActive(true)
                .build();
        FuelSurchargeIndex futureRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("7.00"))
                .effectiveFrom(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        entityManager.persist(currentActive);
        entityManager.persist(futureRecord);
        entityManager.flush();
        entityManager.clear();

        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findCurrentActive();

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("4.10").compareTo(result.get().getRatePercentage()));
        assertEquals(currentActive.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Backward compatibility: Deprecated findLatestActive() returns newest record regardless of effective date")
    void shouldRetainBackwardCompatibilityForDeprecatedFindLatestActive() {
        FuelSurchargeIndex pastRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.90"))
                .effectiveFrom(LocalDateTime.now().minusDays(5))
                .isActive(true)
                .build();
        FuelSurchargeIndex futureRecord = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.80"))
                .effectiveFrom(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();

        entityManager.persist(pastRecord);
        entityManager.persist(futureRecord);
        entityManager.flush();
        entityManager.clear();

        @SuppressWarnings("deprecation")
        Optional<FuelSurchargeIndex> result = fuelSurchargeIndexRepository.findLatestActive();

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("5.80").compareTo(result.get().getRatePercentage()),
                "Deprecated findLatestActive should continue to return highest effectiveFrom without cutoff");
    }
}
