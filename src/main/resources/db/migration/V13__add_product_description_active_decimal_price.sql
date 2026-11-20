-- Adds e-commerce fields to products:
-- description (nullable, up to 500 chars),
-- active flag (default true for all existing rows),
-- and migrates unit_price from REAL to NUMERIC(12,2) for exact monetary arithmetic.
ALTER TABLE products
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN active      BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE products
    ALTER COLUMN unit_price TYPE NUMERIC(12,2) USING unit_price::NUMERIC(12,2);

-- Regenerate the check constraint with the correct type after the column type change.
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_unit_price_check;
ALTER TABLE products ADD CONSTRAINT products_unit_price_check CHECK (unit_price > 0);
