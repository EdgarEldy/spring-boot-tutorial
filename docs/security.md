# Security

## Overview

Security is built on **Spring Security 6** with **stateless JWT authentication**. No session is stored server-side; every request is authenticated independently by inspecting the `Authorization: Bearer <token>` header.

```
Request
  |
  v
JwtAuthFilter (OncePerRequestFilter)
  |-- extract token from Authorization header
  |-- check blacklist (by jti)
  |-- parse + validate JWT signature and expiry
  |-- load UserDetails (UserDetailsServiceImpl)
  |-- check enabled + non-locked
  |-- populate SecurityContext
  |
  v
SecurityFilterChain
  |-- per-endpoint authorization rules (SecurityConfig)
  |-- method-level rules (@PreAuthorize)
```

---

## JWT structure

Access tokens are signed HS256 JWTs with the following claims:

| Claim | Type | Content |
|---|---|---|
| `sub` | String | Username |
| `jti` | String | Unique token ID (UUID) - used for blacklisting |
| `roles` | List\<String\> | e.g. `["ROLE_ADMIN"]` |
| `permissions` | List\<String\> | e.g. `["PERMISSION_PRODUCTS:WRITE"]` |
| `iat` | Instant | Issued at |
| `exp` | Instant | Expiry (default: 1 hour after `iat`) |

**Refresh tokens** are opaque random strings (32 bytes, Base64 URL-safe), stored in the `refresh_tokens` table. They are valid for 7 days.

Both values are configured in `application.yml` via `jwt.expiration-ms` (access) and `jwt.refresh-expiration-ms` (refresh).

---

## Authentication flows

### Register and activate

```
Client                              Server
  |                                   |
  |-- POST /auth/register ----------->|  saves AppUser (enabled=false), sends activation email
  |                                   |
  |<-- 201 UserResponse --------------|
  |                                   |
  | (user clicks link in email)       |
  |                                   |
  |-- GET /auth/activate?token=... -->|  validates token, sets enabled=true
  |                                   |
  |<-- 200 UserResponse --------------|
```

The activation token has a 24-hour TTL. `validated_at` is set on use (the token is never deleted, preserving the audit trail).

### Login

```
Client                              Server
  |                                   |
  |-- POST /auth/login (email+pwd) -->|  loads AppUser by email, authenticates,
  |                                   |  generates access token + refresh token
  |<-- 200 AuthResponse --------------|  { access_token, token_type, refresh_token, expires_in }
```

### Refresh

```
Client                              Server
  |                                   |
  |-- POST /auth/refresh-token ------>|  Authorization: Bearer <refresh_token>
  |   (refresh token in header)       |  validates, rotates: deletes old, issues new pair
  |                                   |
  |<-- 200 AuthResponse --------------|  { new access_token, new refresh_token, expires_in }
```

### Logout

```
Client                              Server
  |                                   |
  |-- POST /auth/logout ------------->|  Authorization: Bearer <access_token>
  |                                   |  blacklists access token jti
  |                                   |  deletes refresh token
  |<-- 200 OK -----------------------|
  |                                   |
  |-- GET /api/v1/customers --------->|  JwtAuthFilter: jti is blacklisted -> 401
  |<-- 401 Unauthorized -------------|
```

### Forgot password / reset password

```
Client                              Server
  |                                   |
  |-- POST /auth/forgot-password ---->|  finds user by email, generates reset token (1h TTL),
  |                                   |  sends email (async, via @Async)
  |<-- 200 OK -----------------------|
  |                                   |
  | (user clicks link in email)       |
  |                                   |
  |-- POST /auth/reset-password ----->|  ?token=<reset_token>, body: { new_password, confirm }
  |                                   |  deletes token, encodes and saves new password
  |<-- 200 OK -----------------------|
```

The password reset token is **deleted on consumption** (single-use by deletion, not by `validated_at`).

---

## Authorization rules

### Endpoint-level (SecurityConfig)

| Resource | GET | POST / PUT / DELETE |
|---|---|---|
| `/api/v1/categories`, `/api/v1/products` | Public | ADMIN |
| `/api/v1/customers`, `/api/v1/orders` | Authenticated | ADMIN |
| `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh-token` | Public | - |
| `/api/v1/auth/activate`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password` | Public | - |
| `/api/v1/auth/me`, `/api/v1/auth/logout`, `/api/v1/auth/token-info` | Authenticated | - |
| `/api/v1/users`, `/api/v1/roles`, `/api/v1/permissions` | ADMIN | ADMIN |
| `/actuator/**` | Public (health only in prod) | - |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public | - |

### Method-level (@PreAuthorize)

`UserController`, `RoleController`, and `PermissionController` are annotated with `@PreAuthorize("hasRole('ADMIN')")` at the class level, adding a second enforcement layer on top of the filter chain rules.

`@EnableMethodSecurity` is configured in `MethodSecurityConfig`. A static `MethodSecurityExpressionHandler` bean injects `CustomPermissionEvaluator` to resolve `hasPermission(target, action)` calls.

### Custom PermissionEvaluator

`CustomPermissionEvaluator` enables fine-grained checks like `@PreAuthorize("hasPermission('PRODUCTS', 'WRITE')")`. It matches the requested `RESOURCE:ACTION` pair against the `PERMISSION_RESOURCE:ACTION` authorities extracted from the JWT claims and loaded into the `SecurityContext`.

---

## Token blacklist

Access tokens are revoked by their `jti` (JWT ID), not their full value. On logout:

1. `JwtService.extractJti(token)` reads the `jti` claim.
2. `BlacklistedTokenService.blacklist(user, token, jti, expiresAt)` saves a row in `blacklisted_tokens`.
3. `TokenCleanupScheduler` deletes expired rows every night at 03:00 to bound table growth.

`JwtAuthFilter` calls `BlacklistedTokenService.isBlacklisted(jti)` on every authenticated request, before populating the `SecurityContext`. A `DataAccessException` on this check is caught and treated as "not blacklisted" (fail-open) to avoid a database outage blocking all authenticated traffic.

---

## Password constraints

Passwords are enforced by the `@ValidPassword` annotation on `RegisterRequest` and `ResetPasswordRequest`:

- Minimum 8 characters
- At least one uppercase letter
- At least one digit
- At least one special character from `@#$%^&+=!`
- No spaces

---

## Sensitive field masking

`LoggingAspect` logs the arguments and return values of every service method. `AuthResponse`, `RegisterRequest`, and `LoginRequest` override `toString()` to mask sensitive fields (`password`, `access_token`, `refresh_token`), so they never appear in application logs.

---

## Production notes

- `jwt.secret` has no default value in `application-prod.yml` (`${JWT_SECRET}` with no fallback). The app refuses to start in prod without it.
- Swagger UI is accessible in all profiles including prod. Restrict or disable it once a real production deployment is set up.
- The structured log format in prod is ECS JSON (`logging.structured.format.console/file: ecs`), suitable for Elastic Stack ingestion.
