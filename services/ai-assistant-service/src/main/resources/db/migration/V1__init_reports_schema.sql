CREATE TABLE daily_delivery_stats (
                                      id BIGSERIAL PRIMARY KEY,
                                      report_date DATE NOT NULL UNIQUE,
                                      total_deliveries INT NOT NULL DEFAULT 0,
                                      successful_deliveries INT NOT NULL DEFAULT 0,
                                      failed_deliveries INT NOT NULL DEFAULT 0,
                                      success_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
                                      failure_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_stats_date ON daily_delivery_stats(report_date);

CREATE TABLE processed_events (
                                  event_id VARCHAR(255) PRIMARY KEY,
                                  processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);