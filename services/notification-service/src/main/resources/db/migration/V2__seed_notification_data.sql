-- ==============================================================================
-- SwiftDrop — Notification Service Initial Seed Data (V2)
-- Provides realistic demonstration records for evaluators upon startup
-- ==============================================================================

INSERT INTO notification_logs (
    event_id, parcel_id, tracking_number, recipient_email, recipient_name,
    notification_type, channel, subject, message_body, delivery_status,
    retry_count, error_message, created_at, sent_at
) VALUES
(
    'evt-seed-001',
    'PRC-2026-001',
    'SWIFT-COL-9901',
    'customer@swiftdrop.io',
    'Alice Fernando',
    'ORDER_CONFIRMED',
    'EMAIL',
    'SwiftDrop: Your parcel #SWIFT-COL-9901 has been booked!',
    'Thank you for booking with SwiftDrop! Your package has been confirmed and scheduled for pickup.',
    'SENT',
    0,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '2 seconds'
),
(
    'evt-seed-002',
    'PRC-2026-001',
    'SWIFT-COL-9901',
    'customer@swiftdrop.io',
    'Alice Fernando',
    'OUT_FOR_DELIVERY',
    'EMAIL',
    'SwiftDrop: Driver Dave is on the way with your parcel!',
    'Good news! Your courier Dave has picked up your package and is on the way to your delivery address.',
    'SENT',
    0,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes' + INTERVAL '1 second'
);

