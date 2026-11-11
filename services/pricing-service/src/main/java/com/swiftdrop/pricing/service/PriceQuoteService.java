package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.PriceQuote;
import com.swiftdrop.pricing.exception.QuoteExpiredException;
import com.swiftdrop.pricing.exception.QuoteNotFoundException;
import com.swiftdrop.pricing.mapper.PriceQuoteMapper;
import com.swiftdrop.pricing.repository.PriceQuoteRepository;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service managing the full lifecycle of price quotes:
 * creation with configurable TTL, persistence with comprehensive entity validation,
 * historical retrieval, active retrieval with expiration enforcement, and TTL cleanup support.
 */
@Service
@Transactional(readOnly = true)
public class PriceQuoteService {

    private final PriceQuoteRepository priceQuoteRepository;
    private final PriceQuoteMapper priceQuoteMapper;
    private final long quoteTtlMinutes;
    private final Clock clock;

    @Autowired
    public PriceQuoteService(
            PriceQuoteRepository priceQuoteRepository,
            PriceQuoteMapper priceQuoteMapper,
            ObjectProvider<Clock> clockProvider,
            @Value("${pricing.quote.ttl-minutes:30}") long quoteTtlMinutes) {
        this(
                priceQuoteRepository,
                priceQuoteMapper,
                quoteTtlMinutes,
                clockProvider != null ? clockProvider.getIfAvailable(Clock::systemDefaultZone) : Clock.systemDefaultZone()
        );
    }

    public PriceQuoteService(
            PriceQuoteRepository priceQuoteRepository,
            PriceQuoteMapper priceQuoteMapper,
            long quoteTtlMinutes,
            Clock clock) {
        this.priceQuoteRepository = priceQuoteRepository;
        this.priceQuoteMapper = priceQuoteMapper;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        // Validate TTL during construction so misconfiguration fails fast at startup
        if (quoteTtlMinutes <= 0) {
            throw new IllegalStateException(
                    "pricing.quote.ttl-minutes must be strictly greater than zero, configured value: " + quoteTtlMinutes);
        }
        this.quoteTtlMinutes = quoteTtlMinutes;
    }

    // -------------------------------------------------------------------------
    // Quote Creation
    // -------------------------------------------------------------------------

    /**
     * Assembles a {@link PriceQuote} from upstream stage results and persists it.
     * Uses a single supplied {@code creationTime} for all timestamp calculations.
     *
     * @param request        the original pricing request DTO
     * @param strategyResult the fare breakdown from the pricing strategy (Step 3)
     * @param zoneMultiplier the resolved zone multiplier (Step 4)
     * @param discountResult the discount and final fare breakdown (Step 5)
     * @param creationTime   the quote creation timestamp (must not be null)
     * @return the persisted {@link PriceQuote}
     */
    @Transactional
    public PriceQuote createAndSaveQuote(
            PricingRequestDto request,
            StrategyPricingResult strategyResult,
            BigDecimal zoneMultiplier,
            DiscountEngineResult discountResult,
            LocalDateTime creationTime) {

        if (request == null) {
            throw new IllegalArgumentException("PricingRequestDto cannot be null");
        }
        if (strategyResult == null) {
            throw new IllegalArgumentException("StrategyPricingResult cannot be null");
        }
        if (discountResult == null) {
            throw new IllegalArgumentException("DiscountEngineResult cannot be null");
        }
        if (zoneMultiplier == null) {
            throw new IllegalArgumentException("Zone multiplier cannot be null");
        }
        if (zoneMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Zone multiplier must be greater than zero");
        }
        if (creationTime == null) {
            throw new IllegalArgumentException("Creation time cannot be null");
        }

        // expiresAt is derived exclusively from the supplied creationTime
        LocalDateTime expiresAt = creationTime.plusMinutes(quoteTtlMinutes);

        PriceQuote quote = PriceQuote.builder()
                .serviceType(request.getServiceType())
                .originZone(request.getOriginZone())
                .destinationZone(request.getDestinationZone())
                .physicalWeightKg(request.getPhysicalWeightKg())
                .volumetricWeightKg(strategyResult.getVolumetricWeightKg())
                .billableWeightKg(strategyResult.getBillableWeightKg())
                .distanceKm(request.getDistanceKm())
                .baseFare(strategyResult.getBaseFare())
                .weightCharge(strategyResult.getWeightCharge())
                .distanceCharge(strategyResult.getDistanceCharge())
                .urgencyMultiplier(strategyResult.getUrgencyMultiplier())
                .zoneMultiplier(zoneMultiplier)
                .fuelSurchargeAmount(discountResult.getFuelSurchargeAmount())
                .merchantDiscountAmount(discountResult.getMerchantDiscountAmount())
                .promoDiscountAmount(discountResult.getPromoDiscountAmount())
                .finalFare(discountResult.getFinalFare())
                // currency uses entity @Builder.Default ("LKR")
                .expiresAt(expiresAt)
                .build();

        return saveQuote(quote);
    }

    /**
     * Overload that obtains {@code creationTime} from the injected clock.
     * Calls the 5-argument method with a single clock reading.
     */
    @Transactional
    public PriceQuote createAndSaveQuote(
            PricingRequestDto request,
            StrategyPricingResult strategyResult,
            BigDecimal zoneMultiplier,
            DiscountEngineResult discountResult) {
        return createAndSaveQuote(request, strategyResult, zoneMultiplier, discountResult, LocalDateTime.now(clock));
    }

    // -------------------------------------------------------------------------
    // Quote Persistence with Full Validation
    // -------------------------------------------------------------------------

    /**
     * Validates and persists a pre-assembled {@link PriceQuote}.
     * Mirrors all entity {@code @NotNull}/{@code @DecimalMin} constraints and V2 CHECK constraints
     * so that no invalid row is ever sent to the database.
     * Rejects already-expired quotes using {@code !LocalDateTime.now(clock).isAfter(expiresAt)}.
     *
     * @param quote the quote to persist
     * @return the saved entity
     */
    @Transactional
    public PriceQuote saveQuote(PriceQuote quote) {
        if (quote == null) {
            throw new IllegalArgumentException("Quote cannot be null");
        }

        // Required string fields
        if (quote.getServiceType() == null) {
            throw new IllegalArgumentException("Quote serviceType cannot be null");
        }
        if (quote.getOriginZone() == null || quote.getOriginZone().isBlank()) {
            throw new IllegalArgumentException("Quote originZone cannot be blank");
        }
        if (quote.getDestinationZone() == null || quote.getDestinationZone().isBlank()) {
            throw new IllegalArgumentException("Quote destinationZone cannot be blank");
        }

        // Weight constraints (mirror V2: physical_weight_kg > 0, volumetric >= 0, billable > 0)
        if (quote.getPhysicalWeightKg() == null || quote.getPhysicalWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quote physicalWeightKg must be greater than zero");
        }
        if (quote.getVolumetricWeightKg() == null || quote.getVolumetricWeightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote volumetricWeightKg cannot be negative");
        }
        if (quote.getBillableWeightKg() == null || quote.getBillableWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quote billableWeightKg must be greater than zero");
        }

        // Distance (mirror V2: distance_km >= 0)
        if (quote.getDistanceKm() == null || quote.getDistanceKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote distanceKm cannot be negative");
        }

        // Fare components (mirror V2: base_fare, weight_charge, distance_charge >= 0)
        if (quote.getBaseFare() == null || quote.getBaseFare().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote baseFare cannot be negative");
        }
        if (quote.getWeightCharge() == null || quote.getWeightCharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote weightCharge cannot be negative");
        }
        if (quote.getDistanceCharge() == null || quote.getDistanceCharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote distanceCharge cannot be negative");
        }

        // Multipliers (mirror V2: urgency_multiplier >= 1.0000, zone_multiplier > 0)
        if (quote.getUrgencyMultiplier() == null
                || quote.getUrgencyMultiplier().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Quote urgencyMultiplier must be >= 1.0000");
        }
        if (quote.getZoneMultiplier() == null || quote.getZoneMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quote zoneMultiplier must be greater than zero");
        }

        // Discount and surcharge amounts (mirror V2: each >= 0)
        if (quote.getFuelSurchargeAmount() == null || quote.getFuelSurchargeAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote fuelSurchargeAmount cannot be negative");
        }
        if (quote.getMerchantDiscountAmount() == null || quote.getMerchantDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote merchantDiscountAmount cannot be negative");
        }
        if (quote.getPromoDiscountAmount() == null || quote.getPromoDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote promoDiscountAmount cannot be negative");
        }

        // Final fare (mirror V2: final_fare >= 0)
        if (quote.getFinalFare() == null || quote.getFinalFare().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quote finalFare cannot be negative");
        }

        // Expiration timestamp — required and must not be already past
        if (quote.getExpiresAt() == null) {
            throw new IllegalArgumentException("Quote expiresAt cannot be null");
        }
        // Using agreed boundary: now > expiresAt means expired; now == expiresAt means still valid
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isAfter(quote.getExpiresAt())) {
            throw new IllegalArgumentException(
                    "Cannot persist an already-expired quote (expiresAt: " + quote.getExpiresAt() + ")");
        }

        return priceQuoteRepository.save(quote);
    }

    // -------------------------------------------------------------------------
    // Historical Retrieval (returns quote regardless of expiry)
    // -------------------------------------------------------------------------

    /**
     * Retrieves a price quote by ID regardless of its expiration status.
     *
     * @param id the quote UUID
     * @return the entity
     * @throws QuoteNotFoundException if no quote with the given ID exists
     */
    public PriceQuote getQuote(UUID id) {
        return priceQuoteRepository.findById(id)
                .orElseThrow(() -> new QuoteNotFoundException(id));
    }

    /**
     * Retrieves a price quote by ID and maps it to a DTO, regardless of expiration.
     */
    public PricingResponseDto getQuoteDto(UUID id) {
        return priceQuoteMapper.toDto(getQuote(id));
    }

    // -------------------------------------------------------------------------
    // Active Retrieval (enforces expiration boundary)
    // -------------------------------------------------------------------------

    /**
     * Retrieves a price quote that has not yet expired as of the given evaluation time.
     * Boundary: a quote with {@code expiresAt == evaluationTime} is considered valid.
     *
     * @param id             the quote UUID
     * @param evaluationTime the timestamp to evaluate expiration against (must not be null)
     * @return the entity
     * @throws IllegalArgumentException if evaluationTime is null
     * @throws QuoteNotFoundException   if no quote with the given ID exists
     * @throws QuoteExpiredException    if the quote exists but evaluationTime is after expiresAt
     */
    public PriceQuote getActiveQuote(UUID id, LocalDateTime evaluationTime) {
        if (evaluationTime == null) {
            throw new IllegalArgumentException("Evaluation time cannot be null");
        }
        PriceQuote quote = priceQuoteRepository.findById(id)
                .orElseThrow(() -> new QuoteNotFoundException(id));
        if (evaluationTime.isAfter(quote.getExpiresAt())) {
            throw new QuoteExpiredException(id, quote.getExpiresAt());
        }
        return quote;
    }

    /**
     * Overload that evaluates active status against the current clock time.
     */
    public PriceQuote getActiveQuote(UUID id) {
        return getActiveQuote(id, LocalDateTime.now(clock));
    }

    /**
     * Retrieves an active price quote and maps it to a DTO.
     */
    public PricingResponseDto getActiveQuoteDto(UUID id) {
        return priceQuoteMapper.toDto(getActiveQuote(id));
    }

    // -------------------------------------------------------------------------
    // TTL / Cleanup Support
    // -------------------------------------------------------------------------

    /**
     * Returns all quotes whose expiration timestamp is strictly before the given cutoff.
     */
    public List<PriceQuote> findExpiredQuotes(LocalDateTime cutoff) {
        return priceQuoteRepository.findByExpiresAtBefore(cutoff);
    }
}
