-- Adds first_name and last_name to app_users. Both columns are nullable
-- so that the migration can run against an existing database (dev, test)
-- that already holds seeded rows without a default value. The API boundary
-- enforces their presence via @NotBlank on RegisterRequest.

ALTER TABLE app_users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100);
