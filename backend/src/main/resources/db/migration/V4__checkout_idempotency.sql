-- Additive migration: historical orders remain readable without new replay metadata.
ALTER TABLE orders ADD COLUMN tracking_token VARCHAR(64);
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE orders ADD COLUMN request_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX uq_orders_idempotency_key ON orders(idempotency_key);
