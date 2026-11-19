CREATE INDEX idx_trades_source_sort ON trades (source_type, id);
CREATE INDEX idx_trades_status_sort ON trades (status, id);
CREATE INDEX idx_trades_validation_reason_sort ON trades (validation_reason, id);
CREATE INDEX idx_trades_received_at_sort ON trades (received_at, id);
