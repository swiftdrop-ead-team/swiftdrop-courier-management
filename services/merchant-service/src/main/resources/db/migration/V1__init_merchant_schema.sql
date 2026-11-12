-- Merchant Service schema. Merchant-owned data stays in swiftdrop_merchant.
CREATE TABLE merchant_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    business_address TEXT NOT NULL,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    review_notes TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE merchant_api_keys (
    id BIGSERIAL PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    key_prefix VARCHAR(20) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    scopes VARCHAR(300) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE merchant_orders (
    id BIGSERIAL PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(100) NOT NULL,
    parcel_id VARCHAR(100) UNIQUE,
    tracking_number VARCHAR(100),
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    delivery_address TEXT NOT NULL,
    pickup_address TEXT NOT NULL,
    description VARCHAR(500),
    weight_kg NUMERIC(8, 2),
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    parcel_status_updated_at TIMESTAMP WITHOUT TIME ZONE,
    status_version BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_merchant_order_idempotency UNIQUE (merchant_id, idempotency_key)
);

CREATE TABLE merchant_webhooks (
    id BIGSERIAL PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    target_url VARCHAR(2048) NOT NULL,
    event_types VARCHAR(1000) NOT NULL,
    signing_secret_encrypted TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE merchant_processed_events (
    event_key VARCHAR(64) PRIMARY KEY,
    parcel_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE merchant_webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL REFERENCES merchant_webhooks(id) ON DELETE CASCADE,
    event_key VARCHAR(64) NOT NULL REFERENCES merchant_processed_events(event_key) ON DELETE RESTRICT,
    order_id BIGINT NOT NULL REFERENCES merchant_orders(id) ON DELETE CASCADE,
    event_payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    last_http_status INT,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_merchant_webhook_event UNIQUE (webhook_id, event_key)
);

CREATE INDEX idx_merchant_orders_owner_created ON merchant_orders(merchant_id, created_at);
CREATE INDEX idx_merchant_orders_parcel_id ON merchant_orders(parcel_id);
CREATE INDEX idx_merchant_webhooks_owner ON merchant_webhooks(merchant_id);
CREATE INDEX idx_merchant_deliveries_due ON merchant_webhook_deliveries(status, next_attempt_at);
CREATE INDEX idx_merchant_deliveries_webhook ON merchant_webhook_deliveries(webhook_id, created_at);
