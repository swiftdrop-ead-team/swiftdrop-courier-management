CREATE TABLE parcel_estimates (
 id UUID PRIMARY KEY,owner_id UUID NOT NULL,payload TEXT NOT NULL,amount NUMERIC(12,2) NOT NULL,
 currency VARCHAR(10) NOT NULL,expires_at TIMESTAMPTZ NOT NULL,consumed_at TIMESTAMPTZ
);
CREATE INDEX idx_parcel_estimates_expiry ON parcel_estimates(expires_at);
