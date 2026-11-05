CREATE TABLE tracking_parcels (
    id BIGSERIAL PRIMARY KEY,
    parcel_id VARCHAR(100) NOT NULL UNIQUE,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    current_status VARCHAR(30) NOT NULL,
    last_event_version BIGINT,
    last_updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE tracking_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    parcel_id VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT
);

CREATE INDEX idx_tracking_events_number_time
    ON tracking_events (tracking_number, occurred_at DESC);
CREATE INDEX idx_tracking_events_parcel_id
    ON tracking_events (parcel_id);
