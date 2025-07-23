CREATE TABLE IF NOT EXISTS payments(
    id UUID PRIMARY KEY NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    processor VARCHAR(10) NOT NULL CHECK (processor IN ('default', 'fallback')),
    requested_at TIMESTAMP NOT NULL
);


