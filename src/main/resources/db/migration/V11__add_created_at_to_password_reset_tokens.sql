-- AuditableToken @MappedSuperclass centralises createdAt in all token entities.
-- password_reset_tokens did not have this column yet; backfill with current time
-- for the rare in-flight rows that may exist in a running system.
ALTER TABLE password_reset_tokens
    ADD COLUMN created_at TIMESTAMP;

UPDATE password_reset_tokens
    SET created_at = NOW()
    WHERE created_at IS NULL;

ALTER TABLE password_reset_tokens
    ALTER COLUMN created_at SET NOT NULL;
