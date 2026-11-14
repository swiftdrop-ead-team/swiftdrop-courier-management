package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.entity.PriceQuote;
import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.mapper.PriceQuoteMapper;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.strategy.PricingContext;
import com.swiftdrop.pricing.strategy.PricingStrategy;
import com.swiftdrop.pricing.strategy.PricingStrategyRegistry;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application service orchestrating the multi-stage pricing pipeline:
 * <ol>
 *   <li>Strategy selection via {@link PricingStrategyRegistry}</li>
 *   <li>Active {@link PricingRule} retrieval from {@link PricingRuleRepository}</li>
 *   <li>Base, weight, distance, and urgency fare computation via {@link PricingStrategy}</li>
 *   <li>Delivery zone lookup and validation via {@link DeliveryZoneService}</li>
 *   <li>Destination zone multiplier and floating fuel surcharge calculation via {@link FuelSurchargeService}</li>
 *   <li>Commercial merchant tier and promotional code discounts via {@link DiscountEngineService}</li>
 *   <li>Price quote assembly, TTL expiration, and persistence via {@link PriceQuoteService}</li>
 *   <li>Entity-to-DTO transformation via {@link PriceQuoteMapper}</li>
 * </ol>
 */
@Service
@Transactional
public class PricingFacadeService {

    private final PricingStrategyRegistry strategyRegistry;
    private final PricingRuleRepository pricingRuleRepository;
    private final DeliveryZoneService deliveryZoneService;
    private final FuelSurchargeService fuelSurchargeService;
    private final DiscountEngineService discountEngineService;
    private final PriceQuoteService priceQuoteService;
    private final PriceQuoteMapper priceQuoteMapper;
    private final Clock clock;

    @Autowired
    public PricingFacadeService(
            PricingStrategyRegistry strategyRegistry,
            PricingRuleRepository pricingRuleRepository,
            DeliveryZoneService deliveryZoneService,
            FuelSurchargeService fuelSurchargeService,
            DiscountEngineService discountEngineService,
            PriceQuoteService priceQuoteService,
            PriceQuoteMapper priceQuoteMapper,
            ObjectProvider<Clock> clockProvider) {
        this(
                strategyRegistry,
                pricingRuleRepository,
                deliveryZoneService,
                fuelSurchargeService,
                discountEngineService,
                priceQuoteService,
                priceQuoteMapper,
                clockProvider != null ? clockProvider.getIfAvailable(Clock::systemDefaultZone) : Clock.systemDefaultZone()
        );
    }

    public PricingFacadeService(
            PricingStrategyRegistry strategyRegistry,
            PricingRuleRepository pricingRuleRepository,
            DeliveryZoneService deliveryZoneService,
            FuelSurchargeService fuelSurchargeService,
            DiscountEngineService discountEngineService,
            PriceQuoteService priceQuoteService,
            PriceQuoteMapper priceQuoteMapper,
            Clock clock) {
        this.strategyRegistry = strategyRegistry;
        this.pricingRuleRepository = pricingRuleRepository;
        this.deliveryZoneService = deliveryZoneService;
        this.fuelSurchargeService = fuelSurchargeService;
        this.discountEngineService = discountEngineService;
        this.priceQuoteService = priceQuoteService;
        this.priceQuoteMapper = priceQuoteMapper;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public PricingFacadeService(
            PricingStrategyRegistry strategyRegistry,
            PricingRuleRepository pricingRuleRepository,
            DeliveryZoneService deliveryZoneService,
            FuelSurchargeService fuelSurchargeService,
            DiscountEngineService discountEngineService,
            PriceQuoteService priceQuoteService,
            PriceQuoteMapper priceQuoteMapper) {
        this(
                strategyRegistry,
                pricingRuleRepository,
                deliveryZoneService,
                fuelSurchargeService,
                discountEngineService,
                priceQuoteService,
                priceQuoteMapper,
                Clock.systemDefaultZone()
        );
    }

    /**
     * Executes the complete pricing pipeline and persists a guaranteed price quote.
     * Evaluates current timestamp using the injected system clock.
     *
     * @param request      the validated pricing request parameters
     * @param merchantTier the server-resolved merchant tier (pass {@code null} if untrusted/unauthenticated)
     * @return the persisted price quote mapped to {@link PricingResponseDto}
     */
    public PricingResponseDto calculateAndCreateQuote(PricingRequestDto request, MerchantTier merchantTier) {
        return calculateAndCreateQuote(request, merchantTier, LocalDateTime.now(clock));
    }

    /**
     * Executes the complete pricing pipeline against an explicit calculation timestamp.
     *
     * @param request         the validated pricing request parameters
     * @param merchantTier    the server-resolved merchant tier (pass {@code null} if untrusted/unauthenticated)
     * @param calculationTime the evaluation timestamp (must not be null)
     * @return the persisted price quote mapped to {@link PricingResponseDto}
     */
    public PricingResponseDto calculateAndCreateQuote(
            PricingRequestDto request,
            MerchantTier merchantTier,
            LocalDateTime calculationTime) {

        if (request == null) {
            throw new IllegalArgumentException("PricingRequestDto cannot be null");
        }
        if (calculationTime == null) {
            throw new IllegalArgumentException("Calculation time cannot be null");
        }

        // 1. Resolve pricing strategy for the requested service delivery urgency tier
        PricingStrategy strategy = strategyRegistry.getStrategy(request.getServiceType());

        // 2. Fetch the active pricing rule configuration for the tier
        PricingRule pricingRule = pricingRuleRepository
                .findByServiceTypeAndIsActiveTrue(request.getServiceType())
                .orElseThrow(() -> new IllegalStateException(
                        "No active pricing rule configured for service type: " + request.getServiceType()));

        // 3. Build pricing context and compute delivery tier strategy fare
        PricingContext context = PricingContext.from(request, calculationTime);
        StrategyPricingResult strategyResult = strategy.calculate(context, pricingRule);

        // 4. Validate delivery zones (both origin and destination)
        DeliveryZone originZone = deliveryZoneService.validateAndGetZone(request.getOriginZone());
        DeliveryZone destinationZone = deliveryZoneService.validateAndGetZone(request.getDestinationZone());

        // 5. Compute destination zone multiplier and floating fuel surcharge
        ZoneAndFuelPricingResult zoneAndFuelResult = fuelSurchargeService.calculateZoneAndFuelPricing(
                strategyResult, originZone, destinationZone, calculationTime);

        // 6. Calculate commercial merchant tier and promo code discounts
        DiscountEngineResult discountResult = discountEngineService.calculateDiscounts(
                zoneAndFuelResult.getZoneAdjustedFare(),
                merchantTier,
                request.getPromoCode(),
                zoneAndFuelResult.getFuelSurchargeAmount(),
                calculationTime);

        // 7. Assemble and persist the price quote entity
        PriceQuote quote = priceQuoteService.createAndSaveQuote(
                request,
                strategyResult,
                zoneAndFuelResult.getZoneMultiplier(),
                discountResult,
                calculationTime);

        // 8. Map persisted entity to client response DTO
        return priceQuoteMapper.toDto(quote);
    }

    /**
     * Retrieves an existing quote by its UUID regardless of expiration status.
     *
     * @param id the quote UUID
     * @return the quote DTO
     */
    @Transactional(readOnly = true)
    public PricingResponseDto getQuote(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Quote ID cannot be null");
        }
        return priceQuoteService.getQuoteDto(id);
    }

    /**
     * Retrieves an active quote by its UUID, enforcing TTL expiration.
     *
     * @param id the quote UUID
     * @return the active quote DTO
     * @throws com.swiftdrop.pricing.exception.QuoteExpiredException if the quote has expired
     */
    @Transactional(readOnly = true)
    public PricingResponseDto getActiveQuote(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Quote ID cannot be null");
        }
        return priceQuoteService.getActiveQuoteDto(id);
    }
}
