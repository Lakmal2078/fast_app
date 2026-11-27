ALTER TABLE customer_credentials ADD COLUMN is_agent INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agent_accounts ADD COLUMN holder TEXT;
ALTER TABLE transactions ADD COLUMN destination_id INTEGER;
ALTER TABLE transactions ADD COLUMN decided_by TEXT;
ALTER TABLE transactions ADD COLUMN decided_at INTEGER;
CREATE INDEX transactions_owner_decisions ON transactions(customer_id, decided_at);
