-- V14 migrated monetary amounts to total_amount (NUMERIC 12,2) and no longer
-- maps the original total column in the Order entity. Drop the NOT NULL
-- constraint so Hibernate inserts succeed without providing a value for this
-- legacy column. The column is kept for schema-level backward compatibility
-- but is considered deprecated.
ALTER TABLE orders ALTER COLUMN total DROP NOT NULL;
