-- Adds optimistic locking support to products: JPA's @Version needs a real
-- column to persist the row's version counter, incremented by Hibernate on
-- every UPDATE so a stale concurrent write is rejected instead of silently
-- overwriting someone else's change.
-- Numbered V3, not V2: V2 is reserved for feature/auth's
-- V2__init_users_and_roles.sql, which this branch does not have yet but
-- will once merged forward, and Flyway version numbers must stay unique
-- and ordered across the whole project regardless of merge order.

ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
