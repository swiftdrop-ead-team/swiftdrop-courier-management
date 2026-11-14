ALTER TABLE couriers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE dispatch_assignments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_dispatch_parcel ON dispatch_assignments(parcel_id);
