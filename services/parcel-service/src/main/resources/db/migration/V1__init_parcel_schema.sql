-- ==============================================================================
-- V1__init_parcel_schema.sql
-- SwiftDrop Parcel Service Database Schema (Member 3)
-- ==============================================================================

-- 1. Parcels Table
CREATE TABLE parcels (
    id UUID PRIMARY KEY,
    tracking_number VARCHAR(30) NOT NULL UNIQUE,
    sender_id UUID NOT NULL,
    sender_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    sender_name VARCHAR(150) NOT NULL,
    sender_phone VARCHAR(30) NOT NULL,
    sender_email VARCHAR(150),
    pickup_address TEXT NOT NULL,
    pickup_city VARCHAR(100) NOT NULL,
    pickup_postal_code VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    recipient_email VARCHAR(150),
    delivery_address TEXT NOT NULL,
    delivery_city VARCHAR(100) NOT NULL,
    delivery_postal_code VARCHAR(20) NOT NULL,
    weight_kg NUMERIC(6, 2) NOT NULL,
    length_cm NUMERIC(6, 2) NOT NULL,
    width_cm NUMERIC(6, 2) NOT NULL,
    height_cm NUMERIC(6, 2) NOT NULL,
    description VARCHAR(500),
    is_fragile BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    delivery_fee NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    assigned_courier_id UUID,
    failure_reason VARCHAR(255),
    special_instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_parcels_tracking_number ON parcels(tracking_number);
CREATE INDEX idx_parcels_sender_id ON parcels(sender_id);
CREATE INDEX idx_parcels_status ON parcels(status);
CREATE INDEX idx_parcels_assigned_courier_id ON parcels(assigned_courier_id);
CREATE INDEX idx_parcels_created_at ON parcels(created_at);

-- 2. Parcel Status History (Audit Trail) Table
CREATE TABLE parcel_status_history (
    id UUID PRIMARY KEY,
    parcel_id UUID NOT NULL REFERENCES parcels(id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by_user_id UUID NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    location VARCHAR(200),
    notes TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_parcel_status_history_parcel_id ON parcel_status_history(parcel_id);
CREATE INDEX idx_parcel_status_history_timestamp ON parcel_status_history(timestamp);

