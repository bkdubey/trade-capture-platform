CREATE TABLE trades (
    id UUID PRIMARY KEY,
    booking_system VARCHAR(100) NOT NULL,
    trade_id BIGINT NOT NULL,
    instrument_id VARCHAR(100) NOT NULL,
    quantity BIGINT NOT NULL,
    trade_price NUMERIC(24, 8) NOT NULL,
    trade_timestamp TIMESTAMPTZ NOT NULL,
    source_type VARCHAR(16) NOT NULL CHECK (source_type IN ('FILE', 'KAFKA')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('VALID', 'INVALID')),
    validation_reason VARCHAR(255),
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT trades_validation_reason_consistent CHECK (
        (status = 'VALID' AND validation_reason IS NULL)
        OR (status = 'INVALID' AND validation_reason IS NOT NULL)
    )
);

CREATE INDEX idx_trades_timestamp_id ON trades (trade_timestamp DESC, id);
CREATE INDEX idx_trades_status_timestamp ON trades (status, trade_timestamp DESC);
CREATE INDEX idx_trades_source_timestamp ON trades (source_type, trade_timestamp DESC);
CREATE UNIQUE INDEX uq_trades_ingestion_identity
    ON trades (source_type, booking_system, trade_id, trade_timestamp);
