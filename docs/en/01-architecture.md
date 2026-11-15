# Architecture

This document describes how the application is structured, how a request travels through
the layers, and the key design decisions that apply to every feature.

---

## Request flow

Every HTTP request follows the same path through the application:

```
HTTP Client
    |
    v
  Controller  <----  AOP: ExecutionTimeAspect (@Around)
    |
    v
 Service (interface)
    |
    v
 ServiceImpl  <----  AOP: LoggingAspect (@Around, masks sensitive fields)
    |
    v
 Repository   <----  AOP: RepositoryAuditAspect (@Before/@After/@AfterReturning/@AfterThrowing)
    |
    v
 PostgreSQL 16
```

AOP aspects run alongside each layer without modifying its logic. The filter chain
sits before the controller and handles authentication (see [03-auth.md](03-auth.md)).

---

## Package structure

```
src/main/java/com/edgareldy/springboottutorial/
  config/           - Spring configuration beans (CacheConfig, CorsConfig, OpenApiConfig, ...)
  controller/       - @RestController classes
  dto/
    common/         - ApiResponse<T>, PageResponse<T>
    category/       - CategoryRequest, CategoryResponse
    product/        - ProductRequest, ProductResponse
    customer/       - CustomerRequest, CustomerResponse
    order/          - OrderRequest, OrderResponse, ...
    auth/           - RegisterRequest, LoginRequest, AuthResponse, ...
    user/           - UserResponse, UpdateProfileRequest, ...
    role/           - RoleRequest, RoleResponse, UpdateRolePermissionsRequest
    permission/     - PermissionRequest, PermissionResponse
  entity/           - JPA entities
    user/           - AppUser, Role, Permission, ActivationToken, ...
  exception/        - ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
  mapper/           - MapStruct mappers
  repository/       - Spring Data JPA repositories
  security/         - JwtService, JwtAuthFilter, SecurityConfig, ...
  service/          - Service interfaces
    impl/           - Service implementations
  aspect/           - LoggingAspect, ExecutionTimeAspect, RepositoryAuditAspect
  validation/       - @ValidPassword, @PasswordsMatch, @Unique custom constraints
  event/            - OrderCreatedEvent, OrderCreatedEventListener
```

---

## Key design decisions

### Contract / implementation pattern for services

Every business operation is declared in an interface (`CategoryService`) and implemented
in a separate class (`CategoryServiceImpl`) in the `service/impl/` package.

- Controllers and tests inject the **interface**, never the implementation class.
- `@Transactional` is placed on the **implementation** methods, not the interface.
- `@Service` is placed on the implementation class.

This decouples callers from the concrete class, makes mocking straightforward, and keeps
the transactional boundary close to the data access code.

### Response envelope

Every controller response - success or error - is wrapped in `ApiResponse<T>`:

```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {}
```

Factory methods `ApiResponse.success(message, data)` and `ApiResponse.error(message, data)`
keep construction consistent. HTTP status is communicated via `ResponseEntity<ApiResponse<T>>`
rather than `@ResponseStatus`, which gives controllers explicit control over the status code.

Paginated lists use `PageResponse<T>` as the `data` field:

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {}
```

This means every list endpoint returns `ResponseEntity<ApiResponse<PageResponse<T>>>`.

### DTOs as Java records

All DTOs are Java `record` types. Records are immutable by design, which eliminates the
risk of accidental mutation between layers. MapStruct generates the mapping code at compile
time from record component names.

JSON field names use `@JsonProperty("snake_case")` on individual record components, which
decouples the JSON contract from the Java naming convention:

```java
public record CategoryResponse(
    Long id,
    String name,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt
) {}
```

This keeps Java names in camelCase (consistent with the rest of the codebase) while exposing
snake_case to API clients.

### Error format - RFC 7807

Error responses use Spring 6's built-in `ProblemDetail` class as the `data` field of
`ApiResponse`. ProblemDetail follows RFC 7807 with fields `type`, `title`, `status`,
`detail`, and `instance`. Validation errors (400) add a `fieldErrors` extension:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "Validation failed",
    "instance": "/api/v1/categories",
    "fieldErrors": {
      "name": "Category name must not be blank"
    }
  }
}
```

### Schema management

`spring.jpa.hibernate.ddl-auto=validate` in every profile. Hibernate only validates that
the schema matches the entity model - it never creates or modifies tables. Flyway is the
single source of truth for schema changes. This prevents silent schema drift in production.
