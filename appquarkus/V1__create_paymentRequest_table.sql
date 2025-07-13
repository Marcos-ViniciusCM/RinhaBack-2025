CREATE TABLE IF NOT EXISTS paymentRequest(
    id UUID PRIMARY KEY NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    processor VARCHAR(10) NOT NULL CHECK (processor IN ('default', 'fallback')),
    requested_at TIMESTAMP NOT NULL
);


/*
SELECT
  processor,
  COUNT(*) AS totalRequests,
  SUM(amount) AS totalAmount
FROM payment_request
WHERE requested_at BETWEEN ? AND ?
GROUP BY processor;

CREATE INDEX idx_requested_at_processor ON payment_request (requested_at, processor);
*/