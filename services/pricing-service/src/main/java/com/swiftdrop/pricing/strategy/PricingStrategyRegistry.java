package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.UnsupportedPricingTierException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry managing delivery pricing strategies using an {@link EnumMap}.
 * Automatically discovers all {@link PricingStrategy} beans and provides O(1) strategy resolution.
 */
@Component
public class PricingStrategyRegistry {

    private final Map<ServiceType, PricingStrategy> strategyMap;

    public PricingStrategyRegistry(List<PricingStrategy> strategies) {
        this.strategyMap = new EnumMap<>(ServiceType.class);
        if (strategies != null) {
            for (PricingStrategy strategy : strategies) {
                ServiceType tier = strategy.getSupportedTier();
                if (tier == null) {
                    throw new IllegalStateException("Strategy cannot have null supported tier: " + strategy.getClass().getName());
                }
                if (strategyMap.containsKey(tier)) {
                    throw new IllegalStateException("Duplicate pricing strategy registered for service tier: " + tier);
                }
                strategyMap.put(tier, strategy);
            }
        }
    }

    /**
     * Retrieves the pricing strategy associated with the given delivery service tier.
     *
     * @param serviceType the delivery service urgency tier
     * @return the matching {@link PricingStrategy}
     * @throws UnsupportedPricingTierException if serviceType is null or not registered
     */
    public PricingStrategy getStrategy(ServiceType serviceType) {
        if (serviceType == null) {
            throw new UnsupportedPricingTierException("Service type cannot be null");
        }
        PricingStrategy strategy = strategyMap.get(serviceType);
        if (strategy == null) {
            throw new UnsupportedPricingTierException("No pricing strategy registered for service tier: " + serviceType);
        }
        return strategy;
    }

    /**
     * Returns an unmodifiable view of the registered strategies.
     */
    public Map<ServiceType, PricingStrategy> getRegisteredStrategies() {
        return Collections.unmodifiableMap(strategyMap);
    }
}
