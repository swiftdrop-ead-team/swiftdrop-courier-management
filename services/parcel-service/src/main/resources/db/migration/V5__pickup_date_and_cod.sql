ALTER TABLE parcels ADD COLUMN pickup_date VARCHAR(10);
ALTER TABLE parcels ADD COLUMN cod_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;
UPDATE parcels SET pickup_date = CAST(CAST(created_at AT TIME ZONE 'Asia/Colombo' AS DATE) AS VARCHAR(10));
ALTER TABLE parcels ADD CONSTRAINT chk_parcel_cod_amount CHECK (cod_amount >= 0 AND cod_amount <= 100000);
