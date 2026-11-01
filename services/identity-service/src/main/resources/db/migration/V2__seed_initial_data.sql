-- ==============================================================================
-- V2__seed_initial_data.sql
-- Seed core roles and baseline reference records
-- ==============================================================================

-- Seed core roles
INSERT INTO roles (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN', 'Platform Administrator with full access'),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_MERCHANT', 'Commercial merchant with parcel dispatch capabilities'),
    ('00000000-0000-0000-0000-000000000003', 'ROLE_COURIER', 'Field courier with delivery management capabilities'),
    ('00000000-0000-0000-0000-000000000004', 'ROLE_CUSTOMER', 'Standard individual retail consumer');
