# Data Model

## EER diagram

```
categories
  id            BIGINT  PK
  category_name VARCHAR(255) NOT NULL
  created_at    TIMESTAMP NOT NULL
  updated_at    TIMESTAMP NOT NULL
        |
        | 1:N
        |
products
  id            BIGINT  PK
  category_id   BIGINT  FK -> categories.id NOT NULL
  product_name  VARCHAR(255) NOT NULL
  unit_price    REAL NOT NULL (> 0)
  version       BIGINT NOT NULL DEFAULT 0
        |
        | 1:N
        |
orders
  id            BIGINT  PK
  customer_id   BIGINT  FK -> customers.id NOT NULL
  product_id    BIGINT  FK -> products.id NOT NULL
  quantity      INT NOT NULL (> 0)
  total         DOUBLE PRECISION NOT NULL
        |
        | N:1
        |
customers
  id            BIGINT  PK
  first_name    VARCHAR(255) NOT NULL
  last_name     VARCHAR(255) NOT NULL
  telephone     VARCHAR(50) NOT NULL
  email         VARCHAR(255) NOT NULL UNIQUE
  address       VARCHAR(255) NOT NULL


app_users
  id              BIGINT  PK
  username        VARCHAR(50) NOT NULL UNIQUE
  email           VARCHAR(255) NOT NULL UNIQUE
  password        VARCHAR(255) NOT NULL
  first_name      VARCHAR(100)
  last_name       VARCHAR(100)
  enabled         BOOLEAN NOT NULL DEFAULT TRUE
  account_locked  BOOLEAN NOT NULL DEFAULT FALSE
        |
        | N:M via role_user
        |
roles
  id        BIGINT  PK
  role_name VARCHAR(50) NOT NULL UNIQUE
        |
        | N:M via role_permission
        |
permissions
  id       BIGINT  PK
  resource VARCHAR(50) NOT NULL
  action   VARCHAR(50) NOT NULL
  UNIQUE (resource, action)


role_user
  role_id  BIGINT  FK -> roles.id  PK
  user_id  BIGINT  FK -> app_users.id  PK

role_permission
  role_id       BIGINT  FK -> roles.id  PK
  permission_id BIGINT  FK -> permissions.id  PK


activation_tokens
  id           BIGINT  PK
  user_id      BIGINT  FK -> app_users.id NOT NULL
  token        VARCHAR(255) NOT NULL UNIQUE
  created_at   TIMESTAMP NOT NULL
  expires_at   TIMESTAMP
  validated_at TIMESTAMP  (NULL = not yet used)

password_reset_tokens
  id          BIGINT  PK
  user_id     BIGINT  FK -> app_users.id NOT NULL
  token       VARCHAR(255) NOT NULL UNIQUE
  type        VARCHAR(255) NOT NULL
  created_at  TIMESTAMP NOT NULL
  expiry_date TIMESTAMP NOT NULL

blacklisted_tokens
  id             BIGINT  PK
  user_id        BIGINT  FK -> app_users.id NOT NULL
  token          VARCHAR(768) NOT NULL
  jti            VARCHAR(255) NOT NULL UNIQUE
  blacklisted_at TIMESTAMP
  created_at     TIMESTAMP NOT NULL
  expires_at     TIMESTAMP

refresh_tokens
  id         BIGINT  PK
  user_id    BIGINT  FK -> app_users.id NOT NULL
  token      VARCHAR(768) NOT NULL UNIQUE
  created_at TIMESTAMP NOT NULL
  expires_at TIMESTAMP NOT NULL
  revoked_at TIMESTAMP  (NULL = still valid)
```

---

## Flyway migration history

| Version | File | What it does |
|---|---|---|
| V1 | `V1__init_schema.sql` | Creates `categories`, `products`, `customers`, `orders` with FKs and indexes |
| V2 | `V2__init_users_and_roles.sql` | Creates `app_users` with an inline `role` enum column (later replaced by V7) |
| V3 | `V3__add_version_to_products.sql` | Adds `version BIGINT` to `products` for optimistic locking (`@Version`) |
| V4 | `V4__add_audit_columns_to_categories.sql` | Adds `created_at` and `updated_at` to `categories` for JPA auditing |
| V5 | `V5__init_roles_and_permissions.sql` | Creates `roles`, `permissions`, and `role_permission` join table |
| V6 | `V6__init_token_tables.sql` | Creates `activation_tokens`, `password_reset_tokens`, and `blacklisted_tokens` |
| V7 | `V7__role_user.sql` | Creates `role_user` join table; seeds ADMIN/USER roles; migrates the `role` enum column to rows in `role_user`; drops the old `role` column; adds `enabled` |
| V8 | `V8__add_account_locked.sql` | Adds `account_locked BOOLEAN` to `app_users` |
| V9 | `V9__add_name_fields_to_app_users.sql` | Adds nullable `first_name` and `last_name` to `app_users` |
| V11 | `V11__add_created_at_to_password_reset_tokens.sql` | Backfills `created_at` on `password_reset_tokens` (column added by `AuditableToken` base class) |
| V12 | `V12__add_refresh_tokens.sql` | Creates `refresh_tokens` table |

> V10 is intentionally absent - the number was reserved during development and never used.

---

## Table notes

**products.version**
Used by JPA's `@Version` for optimistic locking. Hibernate increments this column on every UPDATE. If two concurrent transactions read the same row and both try to update it, the second one finds a version mismatch and Spring throws `ObjectOptimisticLockingFailureException`, mapped to 409 Conflict by `GlobalExceptionHandler`. Clients must echo the current `version` value in every PUT request.

**activation_tokens.validated_at**
Tokens are not deleted after use. Setting `validated_at` marks them as consumed. This preserves an audit trail and prevents reuse: the service rejects any token where `validated_at IS NOT NULL`.

**blacklisted_tokens.jti**
Every JWT carries a unique `jti` claim (UUID). On logout, the `jti` is stored here until the token's natural expiry. `JwtAuthFilter` checks `existsByJti` before populating the `SecurityContext`, so the access token is effectively revoked even though JWTs are stateless.

**refresh_tokens.revoked_at**
Set when the token is consumed (rotated) or when the user logs out. A non-null `revoked_at` means the token is no longer usable, even if `expires_at` is in the future.
