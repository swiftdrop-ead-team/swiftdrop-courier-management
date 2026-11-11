package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.UnsupportedPricingTierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyRegistryTest {

    private PricingStrategy standardStrategy;
    private PricingStrategy expressStrategy;
    private PricingStrategy sameDayStrategy;
    private PricingStrategyRegistry registry;

    @BeforeEach
    void setUp() {
        standardStrategy = new StandardDeliveryPricingStrategy();
        expressStrategy = new ExpressDeliveryPricingStrategy();
        sameDayStrategy = new SameDayDeliveryPricingStrategy();

        registry = new PricingStrategyRegistry(List.of(
                standardStrategy,
                expressStrategy,
                sameDayStrategy
        ));
    }

    @Test
    @DisplayName("Registry successfully resolves strategy for all supported ServiceType values")
    void testResolveAllSupportedTiers() {
        assertSame(standardStrategy, registry.getStrategy(ServiceType.STANDARD));
        assertSame(expressStrategy, registry.getStrategy(ServiceType.EXPRESS));
        assertSame(sameDayStrategy, registry.getStrategy(ServiceType.SAME_DAY));
    }

    @Test
    @DisplayName("Requesting strategy with null ServiceType throws UnsupportedPricingTierException")
    void testNullServiceTypeThrows() {
        UnsupportedPricingTierException ex = assertThrows(
                UnsupportedPricingTierException.class,
                () -> registry.getStrategy(null)
        );
        assertTrue(ex.getMessage().contains("Service type cannot be null"));
    }

    @Test
    @DisplayName("Requesting an unregistered ServiceType throws UnsupportedPricingTierException")
    void testUnregisteredServiceTypeThrows() {
        // Create registry missing SAME_DAY
        PricingStrategyRegistry partialRegistry = new PricingStrategyRegistry(List.of(
                standardStrategy,
                expressStrategy
        ));

        UnsupportedPricingTierException ex = assertThrows(
                UnsupportedPricingTierException.class,
                () -> partialRegistry.getStrategy(ServiceType.SAME_DAY)
        );
        assertTrue(ex.getMessage().contains("No pricing strategy registered for service tier: SAME_DAY"));
    }

    @Test
    @DisplayName("Registering duplicate strategies for the same ServiceType throws IllegalStateException")
    void testDuplicateStrategyRegistrationThrows() {
        PricingStrategy duplicateStandard = new StandardDeliveryPricingStrategy();
        List<PricingStrategy> strategies = List.of(
                standardStrategy,
                duplicateStandard
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PricingStrategyRegistry(strategies)
        );
        assertTrue(ex.getMessage().contains("Duplicate pricing strategy registered for service tier: STANDARD"));
    }

    @Test
    @DisplayName("Registering strategy with null supported tier throws IllegalStateException")
    void testStrategyWithNullTierThrows() {
        PricingStrategy faultyStrategy = new PricingStrategy() {
            @Override
            public ServiceType getSupportedTier() {
                return null;
            }

            @Override
            public StrategyPricingResult calculate(PricingContext context, PricingRule rule) {
                return null;
            }
        };

        List<PricingStrategy> strategies = List.of(faultyStrategy);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PricingStrategyRegistry(strategies)
        );
        assertTrue(ex.getMessage().contains("Strategy cannot have null supported tier"));
    }

    @Test
    @DisplayName("Registry initializes safely with empty or null strategy list")
    void testEmptyOrNullStrategyList() {
        PricingStrategyRegistry emptyRegistry = new PricingStrategyRegistry(Collections.emptyList());
        assertTrue(emptyRegistry.getRegisteredStrategies().isEmpty());

        PricingStrategyRegistry nullRegistry = new PricingStrategyRegistry(null);
        assertTrue(nullRegistry.getRegisteredStrategies().isEmpty());
    }

    @Test
    @DisplayName("Registered strategies map is unmodifiable")
    void testRegisteredStrategiesMapIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () ->
                registry.getRegisteredStrategies().put(ServiceType.STANDARD, standardStrategy)
        );
    }
}
