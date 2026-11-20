-- Link checkout-created orders to the authenticated AppUser who placed them.
-- customer_id already stores the legacy Customer link (kept for backward compatibility);
-- user_id is the new column for the e-commerce checkout flow, where the buyer is
-- an AppUser rather than a plain Customer record.
ALTER TABLE orders
    ADD COLUMN user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL;
