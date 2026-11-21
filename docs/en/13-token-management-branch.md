# Branch: feature/token-management

This branch adds the full token lifecycle that was scaffolded in `feature/auth`: email
activation, password reset, JWT blacklisting on logout, and refresh tokens. It also
introduces three infrastructure patterns that appear frequently in production applications:
asynchronous email sending, scheduled cleanup, and pessimistic locking for concurrent writes.

---

## What you will learn

- How to send email asynchronously with `@Async` and a named task executor
- How to schedule a recurring cleanup job with `@Scheduled`
- How to prevent a race condition on a unique token lookup with `PESSIMISTIC_WRITE` locking
- How to generate cryptographically unpredictable tokens with `SecureRandom`
- How to structure multiple token types as separate entities with dedicated tables

---

## What it delivers

### Token entities

| Entity | Table | Purpose |
|---|---|---|
| `ActivationToken` | `activation_tokens` | Email verification after register |
| `PasswordResetToken` | `password_reset_tokens` | Password reset flow |
| `BlacklistedToken` | `blacklisted_tokens` | Revoked JWT tokens (logout) |
| `RefreshToken` | `refresh_tokens` | Long-lived token for access token renewal |

Flyway V5 and V6 create these tables.

### EmailService

`EmailService` (interface) / `EmailServiceImpl` sends HTML emails via JavaMailSender.
Methods are annotated `@Async("emailTaskExecutor")`. The named executor is configured in
`AsyncConfig` with a bounded thread pool.

Templates (`activate_account.html`, `reset_password.html`) are Thymeleaf HTML files in
`src/main/resources/templates/`. `MimeMessageHelper` constructs the MIME message with
inline variable substitution.

> **@ActiveProfiles("test") requirement:** `EmailServiceImpl` depends on `JavaMailSender`.
> In the `test` profile, `spring.mail.host` is not configured, so `JavaMailSender` is not
> created. Any `@SpringBootTest` class that loads the full context must carry
> `@ActiveProfiles("test")`, otherwise Spring fails to find `JavaMailSender` and throws
> `BeanCreationException`. Controller and unit tests are unaffected because they mock
> `EmailService`.

### SecureTokenGenerator

Generates URL-safe tokens using `SecureRandom` and Base64 URL encoding. Using `SecureRandom`
(not `Random` or `UUID.randomUUID()`) ensures the token is cryptographically unpredictable
and cannot be guessed from the sequence.

### TokenCleanupScheduler

`@Scheduled(cron = "0 0 * * * *")` runs every hour. It deletes expired activation tokens,
password reset tokens, and blacklisted tokens whose expiry is in the past. Keeping stale
tokens in the database is not a security risk (an expired token fails validation), but it
does slow down future lookups.

### BlacklistedTokenService and PESSIMISTIC_WRITE

`BlacklistedTokenRepository.findByJti(String)` is annotated
`@Lock(LockModeType.PESSIMISTIC_WRITE)`. When two concurrent logout requests arrive with
the same token, both attempt to check whether the jti is already blacklisted. Without a
database-level lock, both reads return "not found" simultaneously and both proceed to insert,
causing a unique constraint violation. `PESSIMISTIC_WRITE` serialises the two requests at
the database level.

### JwtAuthFilter updates

`JwtAuthFilter` now extracts the `jti` claim from the token and calls
`blacklistedTokenService.isBlacklisted(jti)` before populating the `SecurityContext`. The
call is wrapped in a `try/catch DataAccessException`. If the database is unavailable, the
filter logs the error and allows the request to proceed rather than breaking authentication
for all users.

### AuthResponse refactoring

`AuthResponse` now carries `access_token`, `refresh_token`, and `expires_in` fields instead
of a single `token`. The `username` field was removed because the client can decode the JWT
to retrieve the email without an extra round trip.

---

## OTP activation and mobile password reset (added later)

The original flow used UUID link tokens for both activation and password reset. This section
documents the migration to OTP-based flows to support mobile clients.

### PasswordResetTokenType enum

A `LINK` / `OTP` discriminator stored in the `password_reset_tokens` table. Flyway V19 adds
the column, backfills existing rows as `LINK`, and adds a CHECK constraint.

### OTP-based account activation

`ActivationTokenServiceImpl` now generates a 6-digit numeric code (via
`SecureTokenGenerator.generateOtp(int)`) with a 15-minute TTL instead of a UUID link.

The `validate(String email, String otp)` method scopes the token lookup to the requesting
user: two accounts that happen to receive the same 6-digit code cannot activate each other.

`activate_account.html` renders the code in large bold text instead of a clickable link.

`AuthController.activate()` changes from `GET ?token=` to `POST` with a JSON body
`{"email": "...", "activation_code": "123456"}` (new `ActivateAccountRequest` record).

### Two password reset flows

`PasswordResetTokenServiceImpl.generate(AppUser, PasswordResetTokenType)` creates either a
UUID token (LINK, 1-hour TTL) or a 6-digit OTP (OTP, 5-minute TTL).

Two distinct methods handle verification:

- `verifyOtp(String email, String otp)` - non-destructive: confirms the code is valid without
  consuming it. Used by the mobile "show new-password screen" step.
- `validateAndConsume(String token)` - destructive: validates and marks the token as used.
  Shared by both web (UUID) and mobile (OTP) confirm steps.

A new `reset_password_otp.html` template displays the OTP code for mobile users.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/user/ActivationToken.java` | Activation token entity |
| `src/main/java/.../entity/user/BlacklistedToken.java` | Blacklisted JWT entity |
| `src/main/java/.../entity/user/PasswordResetToken.java` | Password reset token entity |
| `src/main/java/.../entity/token/PasswordResetTokenType.java` | LINK / OTP discriminator |
| `src/main/java/.../entity/user/RefreshToken.java` | Refresh token entity |
| `src/main/java/.../service/EmailService.java` | Async email interface |
| `src/main/java/.../service/impl/EmailServiceImpl.java` | Thymeleaf + @Async |
| `src/main/java/.../security/SecureTokenGenerator.java` | Cryptographic token + OTP generation |
| `src/main/java/.../scheduler/TokenCleanupScheduler.java` | @Scheduled cleanup |
| `src/main/java/.../service/BlacklistedTokenService.java` | PESSIMISTIC_WRITE lookup |
| `src/main/resources/templates/activate_account.html` | Activation OTP email template |
| `src/main/resources/templates/reset_password.html` | Web reset email template |
| `src/main/resources/templates/reset_password_otp.html` | Mobile reset OTP email template |
| `src/main/resources/db/migration/V5__add_token_tables.sql` | Token tables |
| `src/main/resources/db/migration/V6__add_refresh_tokens.sql` | Refresh token table |
| `src/main/resources/db/migration/V19__add_password_reset_token_type.sql` | OTP type column |
