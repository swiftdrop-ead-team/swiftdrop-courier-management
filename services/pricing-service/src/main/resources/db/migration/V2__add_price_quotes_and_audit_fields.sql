-- ==============================================================================
-- SwiftDrop Pricing Microservice — Schema Enhancements (V2)
-- Managed by Flyway
-- 1. Create price_quotes table with CHECK constraints
-- 2. Alter promo_redemptions.quote_id to UUID with FK to price_quotes (ON DELETE RESTRICT)
-- 3. Add customer_id audit column to promo_redemptions
-- 4. Add database-level CHECK constraints to all tables
-- ==============================================================================

-- 1. Create price_quotes table for time-bound guaranteed pricing quotes
CREATE TABLE IF NOT EXISTS price_quotes (
    id UUID PRIMARY KEY,
    service_type VARCHAR(30) NOT NULL,
    origin_zone VARCHAR(50) NOT NULL,
    destination_zone VARCHAR(50) NOT NULL,
    physical_weight_kg NUMERIC(8,2) NOT NULL CHECK (physical_weight_kg > 0),
    volumetric_weight_kg NUMERIC(8,2) NOT NULL CHECK (volumetric_weight_kg >= 0),
    billable_weight_kg NUMERIC(8,2) NOT NULL CHECK (billable_weight_kg > 0),
    distance_km NUMERIC(8,2) NOT NULL CHECK (distance_km >= 0),
    base_fare NUMERIC(12,2) NOT NULL CHECK (base_fare >= 0),
    weight_charge NUMERIC(12,2) NOT NULL CHECK (weight_charge >= 0),
    distance_charge NUMERIC(12,2) NOT NULL CHECK (distance_charge >= 0),
    urgency_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000 CHECK (urgency_multiplier >= 1.0000),
    zone_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000 CHECK (zone_multiplier > 0),
    fuel_surcharge_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (fuel_surcharge_amount >= 0),
    merchant_discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (merchant_discount_amount >= 0),
    promo_discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (promo_discount_amount >= 0),
    final_fare NUMERIC(12,2) NOT NULL CHECK (final_fare >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'LKR',
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Convert promo_redemptions.quote_id from VARCHAR(100) to UUID and add FK
ALTER TABLE promo_redemptions ALTER COLUMN quote_id TYPE UUID USING quote_id::UUID;

ALTER TABLE promo_redemptions ADD CONSTRAINT fk_promo_redemptions_quote
    FOREIGN KEY (quote_id) REFERENCES price_quotes(id) ON DELETE RESTRICT;

-- 3. Enhance promo_redemptions with customer_id for auditability
ALTER TABLE promo_redemptions ADD COLUMN IF NOT EXISTS customer_id VARCHAR(100);

-- 4. Database-level CHECK constraints for data integrity
-- delivery_zones
ALTER TABLE delivery_zones ADD CONSTRAINT chk_delivery_zones_multiplier
    CHECK (base_zone_multiplier > 0);

-- pricing_rules
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_base_fare
    CHECK (base_fare >= 0);
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_per_kg_rate
    CHECK (per_kg_rate >= 0);
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_per_km_rate
    CHECK (per_km_rate >= 0);
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_base_weight
    CHECK (base_weight_kg > 0);
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_volumetric_divisor
    CHECK (volumetric_divisor > 0);
ALTER TABLE pricing_rules ADD CONSTRAINT chk_pricing_rules_urgency_multiplier
    CHECK (urgency_multiplier >= 1.0000);

-- merchant_tier_discounts
ALTER TABLE merchant_tier_discounts ADD CONSTRAINT chk_merchant_tier_discount
    CHECK (discount_percentage >= 0 AND discount_percentage <= 100);
ALTER TABLE merchant_tier_discounts ADD CONSTRAINT chk_merchant_min_shipments
    CHECK (min_monthly_shipments >= 0);

-- promo_codes
ALTER TABLE promo_codes ADD CONSTRAINT chk_promo_codes_discount_value
    CHECK (discount_value > 0);
ALTER TABLE promo_codes ADD CONSTRAINT chk_promo_codes_min_order
    CHECK (min_order_amount >= 0);
ALTER TABLE promo_codes ADD CONSTRAINT chk_promo_codes_max_cap
    CHECK (max_discount_cap IS NULL OR max_discount_cap > 0);
ALTER TABLE promo_codes ADD CONSTRAINT chk_promo_codes_times_used
    CHECK (times_used >= 0);
ALTER TABLE promo_codes ADD CONSTRAINT chk_promo_codes_usage_limit
    CHECK (usage_limit IS NULL OR usage_limit > 0);

-- promo_redemptions
ALTER TABLE promo_redemptions ADD CONSTRAINT chk_promo_redemptions_discount
    CHECK (discount_amount >= 0);
ALTER TABLE promo_redemptions ADD CONSTRAINT chk_promo_redemptions_order_amt
    CHECK (order_amount >= 0);

-- fuel_surcharge_index
ALTER TABLE fuel_surcharge_index ADD CONSTRAINT chk_fuel_surcharge_rate
    CHECK (rate_percentage >= 0 AND rate_percentage <= 100);

-- 5. Indexes for query optimization
CREATE INDEX IF NOT EXISTS idx_price_quotes_expires_at ON price_quotes(expires_at);
CREATE INDEX IF NOT EXISTS idx_price_quotes_service_type ON price_quotes(service_type);
CREATE INDEX IF NOT EXISTS idx_promo_redemptions_quote ON promo_redemptions(quote_id);
CREATE INDEX IF NOT EXISTS idx_promo_redemptions_customer ON promo_redemptions(customer_id);
