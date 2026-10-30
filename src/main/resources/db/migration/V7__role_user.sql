-- Refactors the user/role relationship from a simple enum column to a proper
-- many-to-many join table, enabling multiple roles per user. Also adds the
-- enabled flag used by the account-activation flow.
--
-- Steps:
--   1. Seed the roles table with the two bootstrap roles so existing users
--      can be migrated without a NOT NULL violation on role_user.
--   2. Add role_user join table.
--   3. Add enabled column (default true for existing users already active).
--   4. Migrate every existing user's enum value to a row in role_user.
--   5. Drop the old enum column and its CHECK constraint.

INSERT INTO roles (role_name) VALUES ('ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (role_name) VALUES ('USER')  ON CONFLICT DO NOTHING;

CREATE TABLE role_user (
    role_id BIGINT NOT NULL REFERENCES roles (id),
    user_id BIGINT NOT NULL REFERENCES app_users (id),
    PRIMARY KEY (role_id, user_id)
);

ALTER TABLE app_users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO role_user (role_id, user_id)
SELECT r.id, u.id
FROM app_users u
JOIN roles r ON r.role_name = u.role;

ALTER TABLE app_users DROP COLUMN role;
