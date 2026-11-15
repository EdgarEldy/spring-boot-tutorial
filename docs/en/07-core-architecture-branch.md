# Branch: feature/core-architecture

This is the first branch. It builds the technical skeleton that every subsequent branch
depends on. No business resources are implemented here; the goal is working infrastructure:
response envelopes, exception handling, AOP aspects, caching configuration, security
scaffolding, and a Docker + CI setup.

---

## What you will learn

- How to build a consistent API response envelope with `ApiResponse<T>` and `PageResponse<T>`
- How to centralise error handling with `@RestControllerAdvice` and RFC 7807 `ProblemDetail`
- How to use all four AOP advice types: `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`, and `@Around`
- How Spring AOP matches proxy methods across the full interface hierarchy (not just the declaring class)
- How to configure Caffeine as a named cache with TTL and size limits via `CacheConfig`
- How to bind configuration properties to a type-safe record using `@ConfigurationProperties`
- How to expose a custom Actuator endpoint with `@Endpoint`
- How to configure CORS, OpenAPI documentation, and JPA auditing as shared infrastructure
- How to containerise the application with Docker Compose and automate CI with GitHub Actions

---

## What it delivers

### ApiResponse and PageResponse

`ApiResponse<T>` is the envelope for every response in the application - success and error
alike. It carries `success`, `message`, `data`, and `timestamp`. Factory methods
`ApiResponse.success(message, data)` and `ApiResponse.error(message, data)` keep
construction consistent across all controllers and exception handlers.

`PageResponse<T>` is used as the `data` field for paginated lists. It wraps Spring Data's
`Page<T>` and exposes `content`, `page`, `size`, `totalElements`, `totalPages`, and `last`.

### GlobalExceptionHandler

`@RestControllerAdvice` with one `@ExceptionHandler` method per exception type. Every handler
returns `ResponseEntity<ApiResponse<ProblemDetail>>`, using Spring 6's native `ProblemDetail`
class rather than a custom DTO. Validation errors (400) add a `fieldErrors` extension map
to the `ProblemDetail` object.

### LoggingAspect

`@Around` pointcut on `execution(* com.edgareldy.springboottutorial.service..*(..))`.
Logs method name, arguments, return value, and execution time at INFO level before and after
each service call. Records that carry passwords or tokens must override `toString()` to mask
sensitive fields - see [06-conventions.md](06-conventions.md).

### ExecutionTimeAspect

`@Around` pointcut on all controller methods. Logs the HTTP endpoint, method name, and
wall-clock execution time. Useful for identifying slow endpoints.

### RepositoryAuditAspect

Uses all four advice types on `execution(* com.edgareldy.springboottutorial.repository..*(..))`:

- `@Before` - logs the method being called with its arguments
- `@AfterReturning` - logs the return value
- `@After` - logs completion (always fires, even on exception)
- `@AfterThrowing` - logs the exception message

> **Spring AOP and interface hierarchies:** A proxy created for `CategoryRepository` (an
> empty interface extending `JpaRepository`) delegates to Spring Data's implementation. When
> `execution()` matches, Spring AOP considers the full interface hierarchy of the proxy, not
> just the declaring class of the method. This means `@Before` on `repository` will fire for
> `save()`, `findById()`, and other methods declared on `CrudRepository`, even though those
> methods are not declared in the `repository` package. This was verified empirically - a
> theoretical analysis based on `Method.getDeclaringClass()` predicted it would not fire,
> but it does.

### CacheConfig

Configures Caffeine as the backing implementation for Spring's `CacheManager`. Two named
caches are defined: `categories` and `products`. Each cache has a maximum size of 500
entries and a time-to-live of 10 minutes. These caches are consumed by `CategoryServiceImpl`
and `ProductServiceImpl` on `feature/products`.

### CorsConfig

Permits cross-origin requests from `http://localhost:4200` (the default Angular dev server
port). Active only when the `dev` profile is loaded.

### AppInfoEndpoint

A custom Actuator endpoint annotated `@Endpoint(id = "appinfo")`. Exposed only in the `dev`
profile via `management.endpoints.web.exposure.include`. Demonstrates how to add custom
Actuator endpoints beyond the built-in ones.

### OpenApiConfig

Configures springdoc-openapi 2.8.16 with a `BearerAuth` JWT security scheme. This prepares
Swagger UI to show an Authorize button that injects the `Authorization: Bearer` header on
authenticated requests, used starting from `feature/auth`.

### JpaAuditingConfig

Enables `@EnableJpaAuditing`, which activates Spring Data's `@CreatedDate` and
`@LastModifiedDate` annotations on entity fields. Configured here so it is available to all
subsequent branches without duplicating the annotation.

### Docker and CI

`docker-compose.yml` starts PostgreSQL 16 and MailHog alongside the application. All
sensitive values (`POSTGRES_*`, `DB_*`, `JWT_SECRET`) are read from environment variables
with safe defaults via `${VAR:-default}`. `.env.example` documents the required variables
without committing actual secrets.

`.github/workflows/ci.yml` runs on every push to `develop` and `master`, and on every pull
request targeting those branches: `mvn clean test` with Testcontainers.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../dto/common/ApiResponse.java` | Response envelope record |
| `src/main/java/.../dto/common/PageResponse.java` | Paginated list wrapper record |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | Centralised error handling |
| `src/main/java/.../exception/ResourceNotFoundException.java` | 404 exception |
| `src/main/java/.../exception/BusinessRuleException.java` | 422 exception |
| `src/main/java/.../aspect/LoggingAspect.java` | Service-level logging |
| `src/main/java/.../aspect/ExecutionTimeAspect.java` | Controller execution time |
| `src/main/java/.../aspect/RepositoryAuditAspect.java` | Repository audit logging |
| `src/main/java/.../config/CacheConfig.java` | Caffeine cache configuration |
| `src/main/java/.../config/CorsConfig.java` | CORS for dev profile |
| `src/main/java/.../config/OpenApiConfig.java` | Swagger UI configuration |
| `src/main/java/.../config/JpaAuditingConfig.java` | JPA auditing enablement |
| `src/main/java/.../config/AppInfoEndpoint.java` | Custom Actuator endpoint |
| `docker-compose.yml` | Local development stack |
| `.env.example` | Environment variable template |
| `.github/workflows/ci.yml` | GitHub Actions pipeline |
