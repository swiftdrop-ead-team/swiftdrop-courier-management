-- ==============================================================================
-- SwiftDrop Dispatch Microservice — Schema Initialization (V1)
-- Managed by Flyway
-- ==============================================================================

-- 1. Couriers table
CREATE TABLE IF NOT EXISTS couriers (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    license_plate VARCHAR(30),
    assigned_zone VARCHAR(50) DEFAULT 'COLOMBO_CENTRAL',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    max_active_deliveries INT NOT NULL DEFAULT 5,
    current_active_deliveries INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Dispatch Assignments table
CREATE TABLE IF NOT EXISTS dispatch_assignments (
    id BIGSERIAL PRIMARY KEY,
    parcel_id VARCHAR(100) NOT NULL,
    courier_id BIGINT NOT NULL REFERENCES couriers(id) ON DELETE RESTRICT,
    tracking_number VARCHAR(50) NOT NULL,
    pickup_address TEXT,
    delivery_address TEXT,
    recipient_name VARCHAR(150),
    recipient_phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    failure_reason TEXT,
    notes TEXT,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Proof of Delivery table
CREATE TABLE IF NOT EXISTS proof_of_delivery (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL UNIQUE REFERENCES dispatch_assignments(id) ON DELETE CASCADE,
    recipient_name VARCHAR(150) NOT NULL,
    recipient_nic VARCHAR(50),
    signature_url TEXT,
    photo_url TEXT,
    delivery_notes TEXT,
    delivered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for optimal lookup performance
CREATE INDEX IF NOT EXISTS idx_couriers_status ON couriers(status);
CREATE INDEX IF NOT EXISTS idx_couriers_assigned_zone ON couriers(assigned_zone);
CREATE INDEX IF NOT EXISTS idx_assignments_courier_id ON dispatch_assignments(courier_id);
CREATE INDEX IF NOT EXISTS idx_assignments_parcel_id ON dispatch_assignments(parcel_id);
CREATE INDEX IF NOT EXISTS idx_assignments_tracking ON dispatch_assignments(tracking_number);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON dispatch_assignments(status);

