-- Exact identifiers from V2; real delivery notifications are unaffected.
DELETE FROM notification_logs
WHERE event_id IN ('evt-seed-001', 'evt-seed-002')
  AND parcel_id = 'PRC-2026-001'
  AND tracking_number = 'SWIFT-COL-9901'
  AND recipient_email = 'customer@swiftdrop.io';
