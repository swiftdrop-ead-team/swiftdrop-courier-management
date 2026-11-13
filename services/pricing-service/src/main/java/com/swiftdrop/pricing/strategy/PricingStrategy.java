package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;

/**
 * Strategy interface defining delivery pricing calculations for specific service urgency tiers.
 */
public interface PricingStrategy {

    /**
     * Returns the service delivery urgency tier supported by this strategy.
     *
     * @return the supported {@link ServiceType}
     */
    ServiceType getSupportedTier();

    /**
     * Calculates the delivery tier pricing based on the provided shipment context and pricing rule.
     *
     * @param context the shipment context containing package dimensions, weight, distance, and zones
     * @param rule    the pricing rule for the supported tier
     * @return the calculated {@link StrategyPricingResult}
     * @throws com.swiftdrop.pricing.exception.DeliveryRestrictionException if delivery limits or conditions are violated
     * @throws IllegalArgumentException if input context, rule, or rule service type is invalid
     */
    StrategyPricingResult calculate(PricingContext context, PricingRule rule);
}
