-- ==============================================================================
-- SwiftDrop Dispatch Microservice — Baseline Courier Seeds (V2)
-- Provides initial operational couriers across primary logistics hubs
-- ==============================================================================

INSERT INTO couriers (user_id, full_name, phone_number, vehicle_type, license_plate, assigned_zone, status, max_active_deliveries, current_active_deliveries)
VALUES
    ('courier-uom-001', 'Kasun Perera', '+94771234567', 'MOTORBIKE', 'WP BDF-1029', 'COLOMBO_CENTRAL', 'AVAILABLE', 8, 0),
    ('courier-uom-002', 'Nimal Silva', '+94719876543', 'VAN', 'WP CAD-4482', 'COLOMBO_SUBURBS', 'AVAILABLE', 15, 0),
    ('courier-uom-003', 'Amara Gunawardena', '+94753334444', 'MOTORBIKE', 'CP BBE-8921', 'KANDY', 'AVAILABLE', 6, 0),
    ('courier-uom-004', 'Dinesh Jayasinghe', '+94765556677', 'THREE_WHEELER', 'WP ABF-5612', 'GAMPAHA', 'AVAILABLE', 10, 0),
    ('courier-uom-005', 'Thilina Bandara', '+94781112233', 'MOTORBIKE', 'SP BCF-7714', 'GALLE', 'OFFLINE', 5, 0)
ON CONFLICT (user_id) DO NOTHING;

