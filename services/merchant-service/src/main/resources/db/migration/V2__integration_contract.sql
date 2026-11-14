ALTER TABLE merchant_orders ADD COLUMN request_fingerprint TEXT;
ALTER TABLE merchant_profiles ALTER COLUMN business_name TYPE VARCHAR(200);
