-- ==============================================================================
-- SwiftDrop Pricing Microservice — Schema Initialization (V1)
-- Managed by Flyway
-- ==============================================================================

-- 1. Delivery Zones table
CREATE TABLE IF NOT EXISTS delivery_zones (
    id BIGSERIAL PRIMARY KEY,
    zone_code VARCHAR(50) NOT NULL UNIQUE,
    zone_name VARCHAR(100) NOT NULL,
    base_zone_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Pricing Rules table (Service urgency tiers: STANDARD, EXPRESS, SAME_DAY)
CREATE TABLE IF NOT EXISTS pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    service_type VARCHAR(30) NOT NULL UNIQUE,
    base_fare NUMERIC(12,2) NOT NULL,
    base_weight_kg NUMERIC(8,2) NOT NULL DEFAULT 1.00,
    per_kg_rate NUMERIC(12,2) NOT NULL,
    per_km_rate NUMERIC(12,2) NOT NULL,
    volumetric_divisor INT NOT NULL DEFAULT 5000,
    urgency_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Merchant Tier Discounts table (Tiers: BRONZE, SILVER, GOLD, PLATINUM)
CREATE TABLE IF NOT EXISTS merchant_tier_discounts (
    id BIGSERIAL PRIMARY KEY,
    tier VARCHAR(30) NOT NULL UNIQUE,
    discount_percentage NUMERIC(5,2) NOT NULL,
    min_monthly_shipments INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 4. Promo Codes table
CREATE TABLE IF NOT EXISTS promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(12,2) NOT NULL,
    min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    max_discount_cap NUMERIC(12,2),
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    usage_limit INT,
    times_used INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 5. Promo Redemptions table (Ensures idempotent redemption & audit trail)
CREATE TABLE IF NOT EXISTS promo_redemptions (
    id BIGSERIAL PRIMARY KEY,
    promo_code_id BIGINT NOT NULL REFERENCES promo_codes(id) ON DELETE RESTRICT,
    promo_code VARCHAR(50) NOT NULL,
    order_reference VARCHAR(100) NOT NULL UNIQUE,
    quote_id VARCHAR(100),
    discount_amount NUMERIC(12,2) NOT NULL,
    order_amount NUMERIC(12,2) NOT NULL,
    redeemed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 6. Fuel Surcharge Index table
CREATE TABLE IF NOT EXISTS fuel_surcharge_index (
    id BIGSERIAL PRIMARY KEY,
    rate_percentage NUMERIC(5,2) NOT NULL,
    effective_from TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by VARCHAR(100) DEFAULT 'SYSTEM',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_delivery_zones_code ON delivery_zones(zone_code);
CREATE INDEX IF NOT EXISTS idx_pricing_rules_service_type ON pricing_rules(service_type);
CREATE INDEX IF NOT EXISTS idx_merchant_tier_discounts_tier ON merchant_tier_discounts(tier);
CREATE INDEX IF NOT EXISTS idx_promo_codes_code ON promo_codes(code);
CREATE INDEX IF NOT EXISTS idx_promo_codes_active_dates ON promo_codes(is_active, valid_from, valid_to);
CREATE INDEX IF NOT EXISTS idx_promo_redemptions_order ON promo_redemptions(order_reference);
CREATE INDEX IF NOT EXISTS idx_promo_redemptions_code ON promo_redemptions(promo_code);
CREATE INDEX IF NOT EXISTS idx_fuel_surcharge_active ON fuel_surcharge_index(is_active);
