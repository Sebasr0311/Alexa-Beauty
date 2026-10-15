-- Add idempotency and replay protection to stock receipts
ALTER TABLE stock_receipts ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE stock_receipts ADD COLUMN request_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX uq_stock_receipts_idempotency_key ON stock_receipts(idempotency_key);
