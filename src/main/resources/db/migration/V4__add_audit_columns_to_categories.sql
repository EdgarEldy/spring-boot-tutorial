-- Backs Category's new @CreatedDate/@LastModifiedDate fields (native Spring
-- Data JPA auditing, enabled by feature/core-architecture's
-- JpaAuditingConfig). NOT NULL with a default so existing/seeded rows stay
-- valid; Hibernate then maintains both columns on every insert/update.

ALTER TABLE categories ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE categories ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
