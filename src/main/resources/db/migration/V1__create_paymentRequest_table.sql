CREATE TABLE IF NOT EXISTS payments(
    correlationId UUID PRIMARY KEY NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    processor VARCHAR(10) NOT NULL,
    requested_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_requested_at ON payments(requested_at);
CREATE INDEX idx_payments_processor_requested_at ON payments(processor, requested_at);

