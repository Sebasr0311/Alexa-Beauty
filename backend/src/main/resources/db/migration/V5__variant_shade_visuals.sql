-- Additive only: existing variants, stock, SKUs and historical snapshots remain unchanged.
ALTER TABLE product_variants ADD COLUMN shade_code VARCHAR(50) NULL;
ALTER TABLE product_variants ADD COLUMN shade_hex VARCHAR(7) NULL;
-- Duplicate combinations are guarded under a product row lock in the service. An
-- unconditional unique index would reject pre-existing legitimate legacy data.

-- New order labels include code + name + presentation; old snapshots are untouched.
ALTER TABLE order_items ALTER COLUMN variant_name TYPE VARCHAR(260);
