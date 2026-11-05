-- ==============================================================================
-- SwiftDrop — Notification Service Database Schema (V1)
-- Tracks multi-channel notifications, delivery logs, idempotency, and retry attempts
-- ==============================================================================

CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100),
    parcel_id VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100),
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    subject VARCHAR(255) NOT NULL,
    message_body TEXT,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITHOUT TIME ZONE
);

-- Indexes for high-performance lookup and idempotency deduplication checks
CREATE INDEX IF NOT EXISTS idx_notification_event_id ON notification_logs(event_id);
CREATE INDEX IF NOT EXISTS idx_notification_parcel_id ON notification_logs(parcel_id);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_email ON notification_logs(recipient_email);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_status ON notification_logs(delivery_status);

