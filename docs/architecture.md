# Architecture

## Layered architecture

```
┌─────────────────────────────────────────────┐
│              HTTP Client / Swagger UI        │
└──────────────────────┬──────────────────────┘
                       │ REST (JSON)
┌──────────────────────▼──────────────────────┐
│                  Controller layer            │
│  @RestController   @RequestMapping /api/v1  │
│  Returns ResponseEntity<ApiResponse<T>>      │
└──────────────────────┬──────────────────────┘
                       │ calls interface
┌──────────────────────▼──────────────────────┐
│                  Service layer               │
│  XxxService (interface) + XxxServiceImpl     │
│  @Service   @Transactional (writes only)     │
└──────────────────────┬──────────────────────┘
                       │ Spring Data JPA
┌──────────────────────▼──────────────────────┐
│               Repository layer              │
│  JpaRepository / JpaSpecificationExecutor   │
│  @Query, @EntityGraph, derived queries      │
└──────────────────────┬──────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────┐
│          PostgreSQL 16 (Docker)              │
│          Schema managed by Flyway            │
└─────────────────────────────────────────────┘
```

Cross-cutting concerns run as AOP aspects alongside every layer (see [AOP](#aop-aspects) below).

---

## Package structure

```
com.edgareldy.springboottutorial/
├── SpringBootTutorialApplication.java
│
├── actuator/           Custom Actuator endpoint (AppInfoEndpoint)
├── aspect/             AOP aspects (LoggingAspect, ExecutionTimeAspect, RepositoryAuditAspect)
├── config/             Spring configuration beans
│   ├── AsyncConfig.java
│   ├── CacheConfig.java
│   ├── CorsConfig.java
│   ├── DataSeeder.java
│   ├── JpaAuditingConfig.java
│   ├── MethodSecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── SchedulingConfig.java
│   └── SecurityConfig.java
│
├── controller/         REST controllers (@RestController)
├── dto/                Data Transfer Objects (Java records, immutable)
│   ├── auth/           RegisterRequest, LoginRequest, AuthResponse, UserResponse, ...
│   ├── common/         ApiResponse<T>, PageResponse<T>
│   ├── category/       CategoryRequest, CategoryResponse
│   ├── customer/       CustomerRequest, CustomerResponse
│   ├── order/          OrderRequest, OrderResponse
│   ├── permission/     PermissionRequest, PermissionResponse
│   ├── product/        ProductRequest, ProductResponse
│   ├── role/           RoleRequest, RoleResponse
│   └── user/           AssignRolesRequest, UpdateProfileRequest
│
├── email/              EmailTemplateName enum, Thymeleaf HTML templates
├── entity/             JPA entities
│   ├── Category, Product, Customer, Order
│   ├── Permission, Role
│   ├── token/          ActivationToken, BlacklistedToken, PasswordResetToken, RefreshToken
│   └── user/           AppUser
│
├── event/              Spring application events
│   ├── OrderCreatedEvent.java
│   └── OrderCreatedEventListener.java
│
├── exception/          Custom exceptions + GlobalExceptionHandler
├── mapper/             MapStruct mappers (AppUserMapper, CategoryMapper, ...)
├── repository/         Spring Data JPA repositories + Specification + Projection
├── scheduler/          @Scheduled tasks (TokenCleanupScheduler)
├── security/           JWT + Spring Security components
│   ├── CustomPermissionEvaluator.java
│   ├── JwtAccessDeniedHandler.java
│   ├── JwtAuthFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtProperties.java
│   ├── JwtService.java
│   ├── SecureTokenGenerator.java
│   └── UserDetailsServiceImpl.java
│
├── service/            Service interfaces (XxxService)
│   └── impl/           Service implementations (XxxServiceImpl, @Service)
│
└── validation/         Custom Jakarta Bean Validation constraints
    ├── password/       @ValidPassword, @PasswordsMatch
    └── unique/         @Unique
```

---

## Key design decisions

### Contract / implementation pattern for services

Every service has a **Java interface** (`XxxService`) in `service/` and an **implementation** (`XxxServiceImpl`) in `service/impl/`. Controllers and tests depend only on the interface, never on the concrete class. `@Transactional` is placed on the implementation, not the interface.

```java
// controller injects the interface
private final CategoryService categoryService;

// implementation owns @Transactional
@Service
public class CategoryServiceImpl implements CategoryService {
    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) { ... }
}
```

### Response envelope

Every response (success and error) is wrapped in `ApiResponse<T>`:

```java
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
    public static <T> ApiResponse<T> success(T data, String message) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

Paginated list endpoints use `ApiResponse<PageResponse<T>>` as the return type, never a bare `List<T>`.

Controllers return `ResponseEntity<ApiResponse<T>>` to control the HTTP status code alongside the body.

### DTOs as Java records

DTOs are immutable Java records - no setters, no default constructor. MapStruct generates the mappers at compile time, matching DTO fields to entity fields by name.

JSON field names are snake_case, Java field names are camelCase. The two are decoupled via `@JsonProperty` on each record component:

```java
public record ProductResponse(
    Long id,
    @JsonProperty("product_name") String productName,
    @JsonProperty("unit_price") double unitPrice,
    @JsonProperty("category_id") Long categoryId
) {}
```

### Error format (RFC 7807)

Error responses use Spring 6's built-in `ProblemDetail` as the `data` field of `ApiResponse`:

```json
{
  "success": false,
  "message": "Product not found",
  "data": {
    "type": "about:blank",
    "title": "Not Found",
    "status": 404,
    "detail": "Product not found with id 99",
    "instance": "/api/v1/products/99"
  },
  "timestamp": "2026-07-02T10:16:05Z"
}
```

Validation errors (400) add a `fieldErrors` extension on `ProblemDetail`:

```json
{
  "data": {
    "status": 400,
    "fieldErrors": { "productName": "must not be blank", "unitPrice": "must be greater than 0" }
  }
}
```

### Schema management

`spring.jpa.hibernate.ddl-auto=validate` in every profile. Hibernate only checks that entity mappings match the schema; it never creates or alters tables. Flyway is the single source of truth for the schema.

---

## AOP aspects

Three aspects run across the application, covering each layer separately:

| Aspect | Pointcut | Advice types | Purpose |
|---|---|---|---|
| `LoggingAspect` | All `@Service` methods | `@Around` | Logs method entry, exit, arguments (sensitive fields masked), and exceptions |
| `ExecutionTimeAspect` | All `@RestController` methods | `@Around` | Measures and logs HTTP request execution time |
| `RepositoryAuditAspect` | All repository methods | `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing` | Traces every repository call, including save results and constraint violations |

`LoggingAspect` masks fields annotated `@SensitiveField` before logging, so passwords and tokens never appear in logs.

---

## Caching

Caffeine cache is configured in `CacheConfig` with two named caches (`categories`, `products`), a 10-minute TTL, and a 500-entry maximum. `@Cacheable` / `@CacheEvict` are applied on `findById`, `update`, and `delete` in `CategoryServiceImpl` and `ProductServiceImpl`. List (paginated) endpoints are intentionally not cached.

---

## Application events

`OrderCreatedEvent` is published by `OrderServiceImpl.create()` inside its `@Transactional` boundary. `OrderCreatedEventListener` consumes it with `@TransactionalEventListener(phase = AFTER_COMMIT)`, ensuring the listener only fires once the order row is durably committed.
