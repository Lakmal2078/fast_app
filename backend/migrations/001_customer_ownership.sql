-- Apply once after schema.sql. Existing transactions remain unassigned.
CREATE TABLE customer_credentials (
    key_hash TEXT PRIMARY KEY NOT NULL,
    customer_id TEXT NOT NULL
);
ALTER TABLE transactions ADD COLUMN customer_id TEXT;
ALTER TABLE transactions ADD COLUMN request_key TEXT;
ALTER TABLE transactions ADD COLUMN request_hash TEXT;
CREATE UNIQUE INDEX transactions_customer_request ON transactions(customer_id, request_key);
CREATE INDEX transactions_customer_time ON transactions(customer_id, timestamp DESC);
