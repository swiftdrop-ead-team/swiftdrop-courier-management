package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.PriceQuote;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.repository.PriceQuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PriceQuoteIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PriceQuoteRepository priceQuoteRepository;

    private static boolean constraintAdded = false;

    @BeforeEach
    void setUp() {
        // In the test profile, Flyway is disabled and Hibernate ddl-auto generates tables
        // from JPA entity annotations. We apply the V2 schema CHECK constraint explicitly
        // to mirror PostgreSQL production schema in H2.
        if (!constraintAdded) {
            try {
                entityManager.getEntityManager().createNativeQuery(
                        "ALTER TABLE price_quotes ADD CONSTRAINT chk_price_quotes_final_fare CHECK (final_fare >= 0)"
                ).executeUpdate();
                constraintAdded = true;
            } catch (Exception ignored) {
                // Constraint may already exist if schema is shared across contexts
                constraintAdded = true;
            }
        }
    }

    private PriceQuote createSampleQuote(LocalDateTime expiresAt) {
        return PriceQuote.builder()
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .volumetricWeightKg(new BigDecimal("0.80"))
                .billableWeightKg(new BigDecimal("2.50"))
                .distanceKm(new BigDecimal("15.00"))
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("200.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .zoneMultiplier(new BigDecimal("1.1500"))
                .fuelSurchargeAmount(new BigDecimal("11.00"))
                .merchantDiscountAmount(new BigDecimal("42.41"))
                .promoDiscountAmount(new BigDecimal("84.81"))
                .finalFare(new BigDecimal("731.91"))
                .currency("LKR")
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    @DisplayName("Persist valid PriceQuote: auto-generates UUID and sets createdAt via @CreationTimestamp")
    void testPersistValidQuote() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PriceQuote quote = createSampleQuote(expiresAt);

        PriceQuote saved = priceQuoteRepository.saveAndFlush(quote);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("LKR", saved.getCurrency());
        assertEquals(ServiceType.STANDARD, saved.getServiceType());
        assertEquals(0, new BigDecimal("731.91").compareTo(saved.getFinalFare()));
    }

    @Test
    @DisplayName("findByIdAndExpiresAtAfter documents strict-greater-than boundary behavior")
    void testStrictGreaterThanBoundary() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        PriceQuote quote = createSampleQuote(now);
        PriceQuote saved = priceQuoteRepository.saveAndFlush(quote);

        // When cutoff equals expiresAt, strict > excludes it -> returns empty
        Optional<PriceQuote> boundaryResult =
                priceQuoteRepository.findByIdAndExpiresAtAfter(saved.getId(), now);
        assertTrue(boundaryResult.isEmpty(), "Strict greater-than should exclude exact boundary");

        // When cutoff is before expiresAt, it is found
        Optional<PriceQuote> beforeResult =
                priceQuoteRepository.findByIdAndExpiresAtAfter(saved.getId(), now.minusSeconds(1));
        assertTrue(beforeResult.isPresent(), "Cutoff before expiresAt should find the quote");
    }

    @Test
    @DisplayName("Historical retrieval: findById returns quote even when expired")
    void testFindById_returnsExpiredQuote() {
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(2);
        PriceQuote quote = createSampleQuote(expiredTime);
        PriceQuote saved = priceQuoteRepository.saveAndFlush(quote);

        Optional<PriceQuote> fetched = priceQuoteRepository.findById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals(saved.getId(), fetched.get().getId());
    }

    @Test
    @DisplayName("findByExpiresAtBefore returns only quotes expired before given cutoff")
    void testFindByExpiresAtBefore() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        PriceQuote expired1 = createSampleQuote(now.minusMinutes(10));
        PriceQuote expired2 = createSampleQuote(now.minusMinutes(5));
        PriceQuote active = createSampleQuote(now.plusMinutes(20));

        priceQuoteRepository.saveAndFlush(expired1);
        priceQuoteRepository.saveAndFlush(expired2);
        priceQuoteRepository.saveAndFlush(active);

        List<PriceQuote> expiredQuotes = priceQuoteRepository.findByExpiresAtBefore(now);
        assertTrue(expiredQuotes.size() >= 2);
        assertTrue(expiredQuotes.stream().allMatch(q -> q.getExpiresAt().isBefore(now)));
    }

    @Test
    @DisplayName("Database CHECK constraint violation (final_fare >= 0) surfaces DataIntegrityViolationException on saveAndFlush")
    void testDatabaseCheckConstraintViolation() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PriceQuote invalidQuote = createSampleQuote(expiresAt);
        invalidQuote.setFinalFare(new BigDecimal("-10.00")); // violates V2 database CHECK (final_fare >= 0)

        // Using saveAndFlush() to ensure SQL INSERT is dispatched to the database
        DataIntegrityViolationException ex = assertThrows(
                DataIntegrityViolationException.class,
                () -> priceQuoteRepository.saveAndFlush(invalidQuote)
        );

        // Verify that the underlying failure is Hibernate's SQL ConstraintViolationException
        // (database-level constraint), distinguishing it from a Jakarta Bean Validation exception
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException,
                "Underlying cause must be org.hibernate.exception.ConstraintViolationException (database constraint), not Bean Validation");
    }
}
