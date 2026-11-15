# Authentication and authorization

This document explains how the application authenticates users and enforces access control
using Spring Security 6, JWT tokens, and role/permission-based authorization.

---

## Overview

The application uses a stateless JWT architecture:

- No server-side session storage. Every request is authenticated by validating the JWT
  in the `Authorization: Bearer` header.
- `JwtAuthFilter` runs before every request and populates the `SecurityContext` if the
  token is valid.
- Roles and permissions are embedded in the JWT claims, eliminating a database lookup
  on every request for most authorization checks.

---

## Authentication flow

```
1. POST /auth/register
   - Saves AppUser with enabled=false
   - Creates ActivationToken, sends activation email via EmailService (@Async)

2. GET /auth/activate-account?token=<token>
   - Validates ActivationToken, sets enabled=true

3. POST /auth/login
   - Verifies password (BCrypt), checks enabled and accountLocked
   - Returns access_token (JWT), refresh_token, expires_in

4. Subsequent requests
   - JwtAuthFilter extracts jti from token
   - Checks BlacklistedToken table (PESSIMISTIC_WRITE)
   - Loads UserDetails, populates SecurityContext

5. POST /auth/logout
   - Blacklists the token's jti in BlacklistedToken table
```

---

## JWT structure

`JwtService.generateToken(AppUser)` builds a token with these claims:

| Claim | Value |
|---|---|
| `sub` | User email |
| `jti` | UUID (unique token id, used for blacklisting) |
| `roles` | List of role names (e.g. `["ADMIN", "USER"]`) |
| `permissions` | List of permission strings (e.g. `["products:read"]`) |
| `iat` | Issued-at timestamp |
| `exp` | Expiry timestamp |

`JwtProperties` (`@ConfigurationProperties(prefix = "jwt")`) reads `jwt.secret` and
`jwt.expiration-ms` from `application.yml`, replacing `@Value` annotations and enabling
constructor-binding validation.

---

## JwtAuthFilter

`JwtAuthFilter` extends `OncePerRequestFilter`. For each request:

1. Reads the `Authorization` header; skips if absent or not `Bearer`.
2. Calls `jwtService.extractEmail(token)` to get the username.
3. Calls `blacklistedTokenService.isBlacklisted(jti)` - wrapped in a `try/catch
   DataAccessException` so a database outage does not break authentication.
4. Loads `UserDetails` via `UserDetailsService`.
5. Calls `jwtService.isTokenValid(token, userDetails)`.
6. Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.

> **@WebMvcTest impact:** `JwtAuthFilter` is a `Filter`. Spring Boot's `@WebMvcTest` slice
> includes every `Filter` bean in the context regardless of `addFilters = false` (that flag
> controls execution, not construction). Every `@WebMvcTest` test class must add
> `@MockitoBean JwtService` and `@MockitoBean UserDetailsService` to allow `JwtAuthFilter`
> to be instantiated, even if the test does not exercise authentication.

---

## SecurityConfig

```
Public endpoints (no token required):
  POST /auth/register
  POST /auth/login
  GET  /auth/activate-account
  POST /auth/forgot-password
  POST /auth/reset-password

Authenticated endpoints:
  GET /auth/me
  POST /auth/logout
  /api/v1/** (all resources)

ADMIN-only controllers (via @PreAuthorize at class level):
  /api/v1/users/**
  /api/v1/roles/**
  /api/v1/permissions/**
```

CSRF is disabled for the REST API (stateless, no browser session). Session creation is
set to `STATELESS`.

---

## Role-based access

`UserDetailsServiceImpl.loadUserByUsername()` loads the `AppUser` with its `Set<Role>` and
each role's `Set<Permission>`, then emits two kinds of `GrantedAuthority`:

- `ROLE_ADMIN`, `ROLE_USER` - for `hasRole()` checks in `SecurityConfig` and `@PreAuthorize`
- `products:read`, `categories:write` - for `hasPermission()` checks via `CustomPermissionEvaluator`

`@EnableMethodSecurity` is enabled in `MethodSecurityConfig`, which registers
`CustomPermissionEvaluator`. This allows fine-grained annotations like:

```java
@PreAuthorize("hasPermission(null, 'products', 'write')")
public ResponseEntity<...> create(...) { ... }
```

---

## Token lifecycle

| Token | Storage | Purpose |
|---|---|---|
| Access token | Client only (memory or localStorage) | Short-lived, authenticates API calls |
| Refresh token | PostgreSQL (`refresh_tokens` table) | Long-lived, used to get a new access token |
| Blacklisted token | PostgreSQL (`blacklisted_tokens` table) | Revoked access tokens (logout) |
| Activation token | PostgreSQL (`activation_tokens` table) | Email verification on register |
| Password reset token | PostgreSQL (`password_reset_tokens` table) | Password reset flow |
