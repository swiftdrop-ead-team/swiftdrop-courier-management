ALTER TABLE parcels ADD COLUMN merchant_order_reference VARCHAR(100);
ALTER TABLE parcels ADD COLUMN merchant_request_fingerprint TEXT;
CREATE UNIQUE INDEX uq_parcel_merchant_reference ON parcels(sender_id,merchant_order_reference) WHERE merchant_order_reference IS NOT NULL;
