# Branch: feature/auth

This branch adds the full authentication layer: Spring Security 6, stateless JWT, user
registration with email activation, and login. It also introduces the two security pitfalls
that are easiest to miss in a tutorial project: sensitive data leaking into logs, and the
impact of adding a `Filter` bean on all existing `@WebMvcTest` tests.

---

## What you will learn

- How to configure Spring Security 6 for a stateless REST API (no sessions, no CSRF)
- How to implement JWT authentication with `jjwt` and `OncePerRequestFilter`
- How to load user details and emit `GrantedAuthority` objects for roles and permissions
- How to use `@AuthenticationPrincipal UserDetails` safely (and why `Authentication.getName()` is not equivalent)
- How to mask sensitive fields in `toString()` to prevent password and token leaks in logs
- How to write an end-to-end security test that uses a real JWT

---

## What it delivers

### AppUser and Role entities

`AppUser` stores `username`, `email`, `password` (BCrypt), `enabled`, and `accountLocked`.
It has a `@ManyToMany` relationship to `Role`. The initial `Role` is an enum (`USER`/`ADMIN`)
that is later replaced by a JPA entity on `feature/roles-permissions`.

Flyway V2 creates the `app_users` and `roles` tables and seeds the initial roles.

### JwtService and JwtProperties

`JwtService` builds and parses JWT tokens using `jjwt` 0.12.6. The token includes `sub`
(email), `jti` (UUID), `roles`, `permissions`, `iat`, and `exp` claims.

`JwtProperties` is a `record` annotated `@ConfigurationProperties(prefix = "jwt")`. It
replaces the two `@Value("${jwt.secret}")` and `@Value("${jwt.expiration-ms}")` fields that
would otherwise scatter configuration reading across the class.

### UserDetailsServiceImpl

`loadUserByUsername(email)` loads the `AppUser` with its roles via `@EntityGraph`. Without
`@EntityGraph`, accessing `appUser.getRoles()` outside a transaction (which is the case in
`JwtAuthFilter`) throws `LazyInitializationException`.

Emitted authorities:
- `ROLE_ADMIN`, `ROLE_USER` from each role name
- `products:read`, `categories:write` etc. from each permission

### JwtAuthFilter

Extends `OncePerRequestFilter`. Extracts the Bearer token, calls `extractEmail`, loads
`UserDetails`, validates the token, and sets `UsernamePasswordAuthenticationToken` in
`SecurityContextHolder`.

The filter depends on `UserDetailsService` (the Spring Security interface), not
`UserDetailsServiceImpl` (the concrete class). This is the only place in the application
where injecting the interface over the implementation matters for testability: mocking
`UserDetailsService` in `@WebMvcTest` only works when the dependency is declared as the
interface.

> **@WebMvcTest impact:** Every `@WebMvcTest` class in the project requires
> `@MockitoBean JwtService` and `@MockitoBean UserDetailsService` after this branch, because
> Spring Boot includes every `Filter` bean in the test context regardless of
> `addFilters = false`. If you create a new `@WebMvcTest` without these two mocks, the test
> will fail to start with `NoSuchBeanDefinitionException`.

### SecurityConfig

```
Stateless configuration:
  - SessionCreationPolicy.STATELESS
  - CSRF disabled
  - JwtAuthFilter added before UsernamePasswordAuthenticationFilter

Public paths:
  POST /auth/register, POST /auth/login, GET /auth/activate-account

Authenticated:
  GET /auth/me, POST /auth/logout, /api/v1/**
```

`JwtAuthenticationEntryPoint` returns a 401 JSON response (not a redirect).
`JwtAccessDeniedHandler` returns a 403 JSON response.

### AuthController and DTO masking

`AuthController.me()` uses `@AuthenticationPrincipal UserDetails userDetails` and calls
`userDetails.getUsername()`.

> **@AuthenticationPrincipal gotcha:** `Authentication.getName()` is documented to return
> the principal's name. The Spring Security `User` class returned by `UserDetailsServiceImpl`
> does not implement `Principal` or `AuthenticatedPrincipal`; it falls back to `toString()`,
> which returns the full `User[Username=..., Password=..., ...]` descriptor. The result is
> that `userRepository.findByUsername(authentication.getName())` finds nothing, because the
> lookup key is a 200-character descriptor instead of a username. Use
> `@AuthenticationPrincipal` to get the `UserDetails` object directly.

`RegisterRequest`, `LoginRequest`, and `AuthResponse` override `toString()` to mask the
`password` and `token` fields. Without this, `LoggingAspect` logs them in plain text to
`logs/app.log`. `AuthDtoMaskingTest` verifies that the overrides are present.

### SecurityAuthorizationTest

`@SpringBootTest(webEnvironment = RANDOM_PORT)` with a real Testcontainers PostgreSQL
container. The test registers a user, logs in, receives a real JWT, and calls a protected
endpoint with that token. This is the only test that exercises `JwtAuthFilter` end-to-end
with a genuine token.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/user/AppUser.java` | User JPA entity |
| `src/main/java/.../security/JwtService.java` | JWT generation and parsing |
| `src/main/java/.../security/JwtProperties.java` | @ConfigurationProperties for JWT |
| `src/main/java/.../security/JwtAuthFilter.java` | OncePerRequestFilter |
| `src/main/java/.../security/UserDetailsServiceImpl.java` | UserDetails loading |
| `src/main/java/.../security/SecurityConfig.java` | Filter chain configuration |
| `src/main/java/.../controller/AuthController.java` | register, login, me, logout |
| `src/main/java/.../dto/auth/RegisterRequest.java` | With masked toString() |
| `src/main/java/.../dto/auth/LoginRequest.java` | With masked toString() |
| `src/main/java/.../dto/auth/AuthResponse.java` | With masked toString() |
| `src/test/java/.../security/AuthDtoMaskingTest.java` | Verifies toString() masking |
| `src/test/java/.../security/SecurityAuthorizationTest.java` | End-to-end JWT test |
| `src/main/resources/db/migration/V2__init_users_and_roles.sql` | Users and roles schema |
