# Spring Boot Tutorial

A complete, hands-on walkthrough of building a CRUD REST API with **Spring Boot 3.5.x** (Spring Framework 6, Java 17), organized into Git branches that progressively cover most of the key concepts in the Spring Boot ecosystem.

The data model follows this EER schema: `categories` → `products` → `orders` ← `customers`.

This document is the **complete specification** of the project: it is meant to be followed step by step (with Claude Code or manually) to implement each branch.

Repository: https://github.com/EdgarEldy/spring-boot-tutorial

## Table of contents

- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Branching strategy](#branching-strategy)
- [Project structure](#project-structure)
- [Standard response format](#standard-response-format)
- [Spring AOP](#spring-aop)
- [feature/core-architecture](#featurecore-architecture)
- [feature/products](#featureproducts)
- [feature/customers](#featurecustomers)
- [feature/orders](#featureorders)
- [feature/auth](#featureauth)
- [feature/roles-permissions](#featureroles-permissions)
- [feature/token-management](#featuretoken-management)
- [feature/user-management](#featureuser-management)
- [feature/product-images](#featureproduct-images)
- [feature/shopping-cart](#featureshopping-cart)
- [feature/order-checkout](#featureorder-checkout)
- [feature/order-notifications](#featureorder-notifications)
- [feature/order-authorization](#featureorder-authorization)
- [feature/integration](#featureintegration)
- [Order of work](#order-of-work)
- [Code conventions](#code-conventions)
- [Concepts covered](#concepts-covered)
- [How to follow this tutorial](#how-to-follow-this-tutorial)

## Tech stack

| Component | Choice |
|---|---|
| Framework | Spring Boot 3.5.x (Spring Framework 6) |
| Language | Java 17 (LTS) |
| Build | Maven |
| Database | PostgreSQL 16 (via Docker Compose) |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| DTO mapping | MapStruct + Lombok |
| Validation | Jakarta Bean Validation |
| API documentation | springdoc-openapi (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Security | Spring Security 6 + JWT (jjwt) |
| Aspect-oriented programming | Spring AOP |
| Cache | Spring Cache abstraction + Caffeine |
| Tests | JUnit 5, Mockito, Testcontainers, AssertJ |
| CI/CD | GitHub Actions |
| Containerization | Docker, docker-compose |

## Data model

Base model (branches `feature/core-architecture` through `feature/orders`):

```
categories (id, category_name)
    │ 1
    │
    │ N
products (id, category_id, product_name, unit_price)
    │ 1
    │
    │ N
orders (id, customer_id, product_id, quantity, total)
    │ N
    │
    │ 1
customers (id, first_name, last_name, telephone, email, address)
```

The e-commerce extension (branches `feature/auth` through `feature/integration`) adds `app_users`, `roles`, `permissions`, token tables, `product_images`, `carts`, `cart_items`, and replaces the single-line order model with an `order_items` snapshot table.

### Column details

**categories**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| category_name | VARCHAR(255) | NOT NULL |

**products**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| category_id | BIGINT | FK → categories.id, NOT NULL |
| product_name | VARCHAR(255) | NOT NULL |
| unit_price | NUMERIC(12,2) | NOT NULL, > 0 — migrated from FLOAT in V13 for exact monetary arithmetic |
| description | VARCHAR(500) | nullable — added in V13 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE — added in V13; excluded from create/update payloads |

**customers**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| first_name | VARCHAR(255) | NOT NULL |
| last_name | VARCHAR(255) | NOT NULL |
| telephone | VARCHAR(50) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE, valid email format |
| address | VARCHAR(255) | NOT NULL |

**orders** (extended in `feature/orders`, `feature/order-checkout`, and `feature/order-authorization`)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| customer_id | BIGINT | FK → customers.id, nullable - legacy CRUD orders only |
| product_id | BIGINT | FK → products.id, nullable - legacy CRUD orders only |
| quantity | INT | nullable - legacy CRUD orders only |
| total | NUMERIC(12,2) | nullable - legacy column, made nullable in V15 after backfill |
| order_number | VARCHAR(36) | NOT NULL UNIQUE — UUID generated at creation, added in V14 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' — PENDING, CONFIRMED, CANCELLED; added in V14 |
| total_amount | NUMERIC(12,2) | NOT NULL — sum of all OrderItem lines; added in V14 |
| user_id | BIGINT | FK → app_users.id, nullable — added in V18; set only for checkout orders; legacy CRUD orders have no user_id |

**order_items** (added in V14)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| order_id | BIGINT | FK → orders.id, NOT NULL |
| product_id | BIGINT | FK → products.id, NOT NULL |
| quantity | INT | NOT NULL, > 0 |
| unit_price | NUMERIC(12,2) | NOT NULL — snapshot at order time |

**product_images** (added in V16)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| product_id | BIGINT | FK → products.id, NOT NULL |
| filename | VARCHAR(255) | NOT NULL |
| url | VARCHAR(512) | NOT NULL |
| is_primary | BOOLEAN | NOT NULL DEFAULT FALSE — partial unique index enforces at most one primary per product |

**carts** (added in V17)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | FK → app_users.id, NOT NULL UNIQUE — one cart per user |

**cart_items** (added in V17)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| cart_id | BIGINT | FK → carts.id, NOT NULL |
| product_id | BIGINT | FK → products.id, NOT NULL |
| quantity | INT | NOT NULL, > 0 |

**app_users** (V2: base columns; V7: role_user join; V8: account_locked; V9: first_name/last_name)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| username | VARCHAR(255) | NOT NULL UNIQUE |
| email | VARCHAR(255) | NOT NULL UNIQUE |
| password | VARCHAR(255) | NOT NULL |
| enabled | BOOLEAN | NOT NULL DEFAULT FALSE |
| account_locked | BOOLEAN | NOT NULL DEFAULT FALSE — added in V8 |
| first_name | VARCHAR(100) | nullable — added in V9 |
| last_name | VARCHAR(100) | nullable — added in V9 |

**roles** (added in V5 as JPA entity, replacing the `Role` enum from `feature/auth`)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR(50) | NOT NULL UNIQUE |

**permissions** (added in V5)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| resource | VARCHAR(100) | NOT NULL |
| action | VARCHAR(50) | NOT NULL |
| UNIQUE | (resource, action) | |

**role_user** (join table, added in V7)
| Column | Type | Constraints |
|---|---|---|
| role_id | BIGINT | FK → roles.id |
| user_id | BIGINT | FK → app_users.id |

**role_permission** (join table, added in V5)
| Column | Type | Constraints |
|---|---|---|
| role_id | BIGINT | FK → roles.id |
| permission_id | BIGINT | FK → permissions.id |

**activation_tokens** (added in V6)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| token | VARCHAR(255) | NOT NULL UNIQUE |
| user_id | BIGINT | FK → app_users.id, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| used | BOOLEAN | NOT NULL DEFAULT FALSE |

**blacklisted_tokens** (added in V6 — no user FK; identifies revoked JWTs by jti)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| jti | VARCHAR(255) | NOT NULL UNIQUE |
| invalidated_at | TIMESTAMP | NOT NULL |

**password_reset_tokens** (added in V6)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| token | VARCHAR(255) | NOT NULL UNIQUE |
| user_id | BIGINT | FK → app_users.id, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| used | BOOLEAN | NOT NULL DEFAULT FALSE |

## Branching strategy

| Branch | Role |
|---|---|
| `master` | Stable, production-ready code. No direct commits only merges from `develop`. |
| `develop` | Integration branch. All `feature/*` branches are merged here before `master`. |
| `feature/core-architecture` | Technical foundation: project structure, configuration, baseline security, Docker, CI. |
| `feature/products` | `Category` and `Product` CRUD. |
| `feature/customers` | `Customer` CRUD. |
| `feature/orders` | `Order` CRUD, business logic linking products and customers. |
| `feature/auth` | Authentication and authorization (Spring Security + JWT). |
| `feature/roles-permissions` | Fine-grained Role/Permission CRUD and method-level security (`@PreAuthorize`, custom `PermissionEvaluator`). |
| `feature/token-management` | Activation, blacklisted and password-reset tokens; async email (`@Async`); scheduled cleanup (`@Scheduled`). |
| `feature/user-management` | `AppUser` CRUD (admin), user-role assignment (`role_user` join table, V7), account activation/password-reset flows. |
| `feature/product-images` | Product image upload: `ProductImage` entity (V15), `FileStorageService`, `POST`/`DELETE /api/v1/products/{id}/images` (ROLE_ADMIN). |
| `feature/shopping-cart` | Persistent shopping cart: `Cart`/`CartItem` entities (V16), full CRUD endpoints tied to the authenticated user (`@AuthenticationPrincipal`). |
| `feature/order-checkout` | Atomic checkout: `POST /api/v1/orders` drains the cart and creates an `Order` with `OrderItem` lines in a single transaction. Introduces `order_items` table (V14), `order_number`, `status`, `total_amount`. |
| `feature/order-notifications` | Email notification on order placement via `OrderEmailService` (`@Async`) and `NotificationProperties` (`@ConfigurationProperties`). |
| `feature/order-authorization` | Role-based order access: users see only their own orders, admins see all; `PATCH /api/v1/orders/{id}/status` restricted to ROLE_ADMIN. |
| `feature/integration` | Merges `feature/product-images` into the main chain; end-to-end `EcommerceIntegrationTest` covering the full flow from registration to order status update. |

Each feature is developed on its own branch, then merged into `develop` via a documented **Pull Request** (even solo), to keep a clear, educational trace of each step. In practice, on this particular repository, merges have been done locally (`git merge`) rather than through an actual GitHub Pull Request: this section describes the intended workflow for anyone forking or following along, not necessarily every merge already present in this history.

## Project structure

The tree below shows the full final state across all branches.

```
spring-boot-tutorial/
├── src/
│   ├── main/
│   │   ├── java/com/edgareldy/springboottutorial/
│   │   │   ├── SpringBootTutorialApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── MethodSecurityConfig.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── SchedulingConfig.java
│   │   │   │   └── StorageConfig.java
│   │   │   ├── entity/
│   │   │   │   ├── Category.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── ProductImage.java
│   │   │   │   ├── Customer.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── OrderStatus.java (enum)
│   │   │   │   ├── Cart.java
│   │   │   │   ├── CartItem.java
│   │   │   │   └── user/
│   │   │   │       ├── AppUser.java
│   │   │   │       ├── Role.java
│   │   │   │       ├── Permission.java
│   │   │   │       ├── ActivationToken.java
│   │   │   │       ├── BlacklistedToken.java
│   │   │   │       └── PasswordResetToken.java
│   │   │   ├── repository/
│   │   │   │   ├── CategoryRepository.java
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── ProductImageRepository.java
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── CartRepository.java
│   │   │   │   ├── CartItemRepository.java
│   │   │   │   ├── AppUserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   ├── PermissionRepository.java
│   │   │   │   ├── ActivationTokenRepository.java
│   │   │   │   ├── BlacklistedTokenRepository.java
│   │   │   │   └── PasswordResetTokenRepository.java
│   │   │   ├── dto/
│   │   │   │   ├── common/
│   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   └── PageResponse.java
│   │   │   │   ├── category/ (CategoryRequest, CategoryResponse)
│   │   │   │   ├── product/ (ProductRequest, ProductResponse)
│   │   │   │   ├── image/ (ProductImageResponse)
│   │   │   │   ├── customer/ (CustomerRequest, CustomerResponse)
│   │   │   │   ├── order/ (OrderRequest, OrderResponse, OrderItemResponse, OrderStatusUpdateRequest)
│   │   │   │   ├── cart/ (CartItemRequest, CartItemUpdateRequest, CartItemResponse, CartResponse)
│   │   │   │   ├── role/ (RoleRequest, RoleResponse, UpdateRolePermissionsRequest)
│   │   │   │   ├── permission/ (PermissionRequest, PermissionResponse)
│   │   │   │   ├── user/ (UserResponse, UpdateProfileRequest, UpdateUserRolesRequest)
│   │   │   │   └── auth/ (RegisterRequest, LoginRequest, AuthResponse)
│   │   │   ├── mapper/
│   │   │   │   ├── CategoryMapper.java
│   │   │   │   ├── ProductMapper.java
│   │   │   │   ├── ProductImageMapper.java
│   │   │   │   ├── CustomerMapper.java
│   │   │   │   ├── OrderMapper.java
│   │   │   │   ├── CartMapper.java
│   │   │   │   ├── RoleMapper.java
│   │   │   │   ├── PermissionMapper.java
│   │   │   │   └── AppUserMapper.java
│   │   │   ├── service/
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── ProductImageService.java
│   │   │   │   ├── CustomerService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── OrderEmailService.java
│   │   │   │   ├── CartService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── RoleService.java
│   │   │   │   ├── PermissionService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── ActivationTokenService.java
│   │   │   │   ├── PasswordResetTokenService.java
│   │   │   │   ├── BlacklistedTokenService.java
│   │   │   │   └── impl/
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── ProductServiceImpl.java
│   │   │   │       ├── LocalFileStorageService.java
│   │   │   │       ├── ProductImageServiceImpl.java
│   │   │   │       ├── CustomerServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       ├── OrderEmailServiceImpl.java
│   │   │   │       ├── CartServiceImpl.java
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       ├── UserServiceImpl.java
│   │   │   │       ├── RoleServiceImpl.java
│   │   │   │       ├── PermissionServiceImpl.java
│   │   │   │       ├── EmailServiceImpl.java
│   │   │   │       ├── ActivationTokenServiceImpl.java
│   │   │   │       ├── PasswordResetTokenServiceImpl.java
│   │   │   │       └── BlacklistedTokenServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── CustomerController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── CartController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── RoleController.java
│   │   │   │   └── PermissionController.java
│   │   │   ├── exception/
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── InvalidTokenException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtProperties.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAccessDeniedHandler.java
│   │   │   │   └── CustomPermissionEvaluator.java
│   │   │   ├── validation/
│   │   │   │   ├── password/ (ValidPassword, PasswordValidator, PasswordsMatch, PasswordsMatchValidator)
│   │   │   │   └── unique/ (Unique, UniqueValidator)
│   │   │   ├── event/
│   │   │   │   ├── OrderCreatedEvent.java
│   │   │   │   └── OrderCreatedEventListener.java
│   │   │   ├── aspect/
│   │   │   │   ├── LoggingAspect.java
│   │   │   │   ├── ExecutionTimeAspect.java
│   │   │   │   └── RepositoryAuditAspect.java
│   │   │   ├── token/
│   │   │   │   ├── SecureTokenGenerator.java
│   │   │   │   └── TokenCleanupScheduler.java
│   │   │   └── actuator/
│   │   │       └── AppInfoEndpoint.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__init_schema.sql
│   │           ├── V2__init_users_and_roles.sql
│   │           ├── V3__add_product_version.sql
│   │           ├── V4__add_category_audit_columns.sql
│   │           ├── V5__roles_permissions.sql
│   │           ├── V6__tokens.sql
│   │           ├── V7__role_user.sql
│   │           ├── V8__add_account_locked.sql
│   │           ├── V9__add_name_fields_to_app_users.sql
│   │           ├── V13__add_product_description_active_decimal_price.sql
│   │           ├── V14__evolve_orders_domain.sql
│   │           ├── V15__make_orders_total_legacy_nullable.sql
│   │           ├── V16__create_product_images.sql
│   │           ├── V17__create_cart_tables.sql
│   │           └── V18__add_user_id_to_orders.sql
│   └── test/
│       └── java/com/edgareldy/springboottutorial/
│           ├── controller/ (MockMvc tests)
│           ├── service/ (Mockito unit tests)
│           ├── repository/ (@DataJpaTest / Testcontainers)
│           └── integration/ (EcommerceIntegrationTest, OrderAuthorizationTest, SecurityAuthorizationTest)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

## Standard response format

Every API response (success and error alike) is wrapped in a generic `ApiResponse<T>` DTO, defined in `dto/common/ApiResponse.java`, to keep a consistent contract across all endpoints.

```java
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

- On list endpoints, `data` holds a `PageResponse<T>` (paginated content: `content`, `page`, `size`, `totalElements`, `totalPages`) instead of a plain `List<T>`.
- On error, `GlobalExceptionHandler` returns an `ApiResponse<ProblemDetail>` with `success=false`: `data` holds a standard RFC 7807 `ProblemDetail` (Spring 6's built-in class, `org.springframework.http.ProblemDetail`), not a project-specific error DTO. Field validation errors are attached as a `fieldErrors` extension property.
- Every DTO field is exposed in the JSON contract as snake_case (e.g. `product_name`, `unit_price`, `category_id`), decoupled from the Java field's own camelCase name via an explicit `@JsonProperty` on each record component. Controllers return `ResponseEntity<ApiResponse<T>>`, not `ApiResponse<T>` directly: `ApiResponse<T>` still wraps every response body, `ResponseEntity` only adds control over the HTTP status code alongside it.
- Success response example:

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { "id": 12, "product_name": "Mechanical keyboard", "unit_price": 79.99, "category_id": 3 },
  "timestamp": "2026-07-02T10:15:30Z"
}
```

- Error response example:

```json
{
  "success": false,
  "message": "Product not found",
  "data": {
    "type": "about:blank",
    "title": "Not Found",
    "status": 404,
    "detail": "Product not found",
    "instance": "/api/v1/products/99"
  },
  "timestamp": "2026-07-02T10:16:05Z"
}
```

## Spring AOP

The project uses **Spring AOP** to illustrate aspect-oriented programming, kept out of any business logic in services/controllers.

- Dependency: `spring-boot-starter-aop`
- `LoggingAspect` (`aspect/LoggingAspect.java`): `@Around` advice on all `@Service` beans (pointcut `execution(* com.edgareldy.springboottutorial.service..*(..))`), logs method entry/exit, arguments, and thrown exceptions
- `ExecutionTimeAspect` (`aspect/ExecutionTimeAspect.java`): `@Around` advice on controller methods, measures and logs the execution time of each HTTP request
- `RepositoryAuditAspect` (`aspect/RepositoryAuditAspect.java`): on the repository layer, the four advice types the other two aspects don't use: `@Before`/`@After` around every repository call, `@AfterReturning` on `save` specifically, `@AfterThrowing` on any repository exception
- Can later be reused for auditing (e.g. tracing who created/modified an order) without polluting business code

## feature/core-architecture

Technical foundation shared by the whole project, to be merged first into `develop`. Originally named `feature/config`; renamed to better reflect that it lays out the whole architectural skeleton (config, cross-cutting concerns, containerization, CI), not just configuration files.

### Tasks

- [x] Initialize the project via Spring Initializr (Maven, Java 17, Spring Boot 3.5.16)
- [x] Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-aop`, `spring-boot-starter-mail`, `spring-boot-devtools`, `flyway-core`, `flyway-database-postgresql`, `postgresql` driver, `lombok`, `mapstruct` + `mapstruct-processor`, `springdoc-openapi-starter-webmvc-ui`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`
- [x] Test dependencies: `spring-boot-starter-test`, `spring-security-test`, `testcontainers` (junit-jupiter, postgresql)
- [x] Create the package tree shown above
- [x] `application.yml`: shared configuration (app name, port, JSON date format)
- [x] `application-dev.yml`: local datasource, `spring.jpa.show-sql=true`, Flyway enabled, `spring.mail` pointing to MailHog (`localhost:1025`)
- [x] `application-test.yml`: Testcontainers datasource
- [x] `application-prod.yml`: datasource via environment variables, JSON logs, `spring.mail` via env vars (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`)
- [x] Flyway script `V1__init_schema.sql` (categories, products, customers, orders tables with FKs)
- [x] `InvalidTokenException`: custom unchecked exception for expired or invalid tokens (used by `feature/token-management`)
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`): handles `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400, field details), `BusinessRuleException` (422), `InvalidTokenException` (400), `BadCredentialsException` (401), `LockedException` (401), `DisabledException` (401), `AccessDeniedException` (403), `AuthenticationException` (401), `ObjectOptimisticLockingFailureException` (409), generic `Exception` (500)
- [x] Error payload as a standard RFC 7807 `ProblemDetail` (`org.springframework.http.ProblemDetail`): `type`, `title`, `status`, `detail`, `instance`, plus a `fieldErrors` extension property on validation failures
- [x] Generic `ApiResponse<T>` (`dto/common/ApiResponse.java`) and `PageResponse<T>` (`dto/common/PageResponse.java`) DTOs, used to wrap every controller response
- [x] `LoggingAspect` and `ExecutionTimeAspect` (`aspect/`): logging and execution-time measurement via Spring AOP (see [Spring AOP](#spring-aop)); `LoggingAspect` uses reflection-based masking to redact fields annotated `@SensitiveField` before logging arguments and return values
- [x] `OpenApiConfig`: title, description, version, JWT security scheme in Swagger UI
- [x] Actuator: expose `health`, `info`, `metrics` in dev (plus a custom `appinfo` endpoint, see `AppInfoEndpoint`); `health` only in prod
- [x] Structured logging (`logback-spring.xml` and Actuator ECS/JSON config, combined)
- [x] `CorsConfig`: allow `localhost:4200` in dev; allowed methods include `PATCH`
- [x] `AsyncConfig`: `@EnableAsync` with a dedicated `ThreadPoolTaskExecutor` for email-sending tasks
- [x] `SchedulingConfig`: `@EnableScheduling` to activate `@Scheduled` tasks (used by `feature/token-management`)
- [x] `JpaAuditingConfig` (`@EnableJpaAuditing`): native Spring Data JPA auditing support, consumed by `Category` on `feature/products`
- [x] `spring-boot-devtools`: automatic restart and other dev-only conveniences, disabled outside local runs
- [x] Multi-stage `Dockerfile` (Maven build + lightweight JRE image)
- [x] `docker-compose.yml`: `app` service + `db` service (PostgreSQL 16) + `mailhog` service (local SMTP for email testing) with volumes and environment variables read from `.env`
- [x] `.env.example`: template listing all required environment variables including `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- [x] `.github/workflows/ci.yml`: Maven build + tests on every push/PR
- [x] Branch `README` explaining the configuration choices (see below)

### Configuration notes

- **Spring Boot 3.5.16, not 4.1.x.** The project was first scoped around Spring Boot 4.1 / Spring Framework 7, but springdoc-openapi had no release compatible with Spring Framework 7 on Maven Central at implementation time. Spring Boot 3.5.16 (Spring Framework 6) is fully compatible with every required dependency (springdoc, MapStruct, jjwt) and is still an actively maintained line, so it was chosen instead.
- **`dependencyManagement` explicitly imports `spring-boot-dependencies`** as `${project.parent.version}`, in addition to inheriting it via `<parent>`. This is redundant (the parent POM already provides the same bill of materials) but was requested explicitly to make version management visible directly in `dependencyManagement`.
- **`hibernate.ddl-auto` is `validate` in every profile**, never `update` or `create`. Flyway's `V1__init_schema.sql` is the single source of truth for the schema; Hibernate only checks that entity mappings agree with it. This avoids the classic drift where a forgotten entity annotation silently alters the schema in one environment but not another.
- **Structured logging combines two mechanisms on purpose.** `logback-spring.xml` includes Spring Boot's own default appenders (`defaults.xml`, `console-appender.xml`) instead of redefining them, and adds a prod-only `file-appender.xml` include via `<springProfile name="prod">`. The actual JSON formatting comes from Spring Boot 3.4+'s native `logging.structured.format.console`/`.file` properties (set to `ecs` in `application-prod.yml`), so no extra dependency (e.g. `logstash-logback-encoder`) or hand-written JSON encoder was needed.
- **`CorsConfig` is annotated `@Profile("dev")`.** Only the local Angular dev server (`localhost:4200`) gets a CORS exemption; prod is expected to declare its own, narrower policy once a real frontend origin exists.
- **Testcontainers only, no H2.** `application-test.yml` declares no datasource at all: `TestcontainersConfiguration`'s `@ServiceConnection` `PostgreSQLContainer` bean wires the datasource automatically. Using real PostgreSQL in tests (instead of an in-memory H2 database) avoids behavioral differences between the two engines (types, constraints, SQL dialect) that would otherwise only surface in production.
- **`GlobalExceptionHandler` returns `ApiResponse<ProblemDetail>`, not a project-specific error DTO.** `ProblemDetail` is Spring 6's built-in RFC 7807 type; using it directly as `ApiResponse<T>`'s type parameter demonstrates the standard without abandoning the project's own envelope, since `ApiResponse<T>` still wraps every response (success and error alike). Only the shape of `data` on errors changed; every success response (`ApiResponse<CategoryResponse>`, `ApiResponse<PageResponse<ProductResponse>>`, etc.) is untouched.

## feature/products

Includes `Category` and `Product`, given their direct link in the model.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/categories` | Paginated list of categories |
| GET | `/api/v1/categories/{id}` | Category detail, including its associated `products` |
| POST | `/api/v1/categories` | Create a category |
| PUT | `/api/v1/categories/{id}` | Update a category |
| DELETE | `/api/v1/categories/{id}` | Delete a category |
| GET | `/api/v1/products` | Paginated list, filterable by `categoryId`, `productName` (contains), `minPrice`/`maxPrice`, sortable by `productName`/`unitPrice` |
| GET | `/api/v1/products/{id}` | Product detail |
| POST | `/api/v1/products` | Create a product |
| PUT | `/api/v1/products/{id}` | Update a product |
| DELETE | `/api/v1/products/{id}` | Delete a product |

### Tasks

- [x] `Category`, `Product` entities with `@OneToMany`/`@ManyToOne` relation (`LAZY` loading)
- [x] `CategoryRepository`, `ProductRepository` (Spring Data JPA) with derived queries (`findByCategoryId`) and one `@Query` using `JOIN FETCH`
- [x] DTOs `CategoryRequest`/`CategoryResponse`, `ProductRequest`/`ProductResponse`
- [x] `Product.unitPrice` uses `BigDecimal` (Java) backed by `NUMERIC(12,2)` (SQL, V13 migration) for exact monetary arithmetic
- [x] `Product.description` (nullable `VARCHAR(500)`) and `Product.active` (boolean, default `true`, V13 migration)
- [x] Corresponding MapStruct mappers
- [x] `CategoryService`, `ProductService` interfaces (contracts) and their `CategoryServiceImpl`, `ProductServiceImpl` implementations in `service/impl`, annotated `@Service`, with `@Transactional` on writes
- [x] Business rule: a category containing products cannot be deleted (`BusinessRuleException`)
- [x] `CategoryController`, `ProductController`: REST CRUD + pagination (`Pageable`) + sorting
- [x] OpenAPI documentation (`@Operation`, `@ApiResponse`) on every endpoint
- [x] Service unit tests (Mockito)
- [x] `@DataJpaTest` repository tests
- [x] MockMvc controller integration tests
- [x] Optimistic locking (`@Version`) on `Product`, with `OptimisticLockingFailureException` mapped to 409
- [x] Dynamic filtering (`Specification`/`JpaSpecificationExecutor`) on `Product`: `productName`/`minPrice`/`maxPrice`, in addition to `categoryId`
- [x] Native JPA auditing (`@CreatedDate`/`@LastModifiedDate`) on `Category`, via `feature/core-architecture`'s `JpaAuditingConfig`
- [x] Controllers return `ResponseEntity<ApiResponse<T>>`; every DTO field is exposed in JSON as snake_case via `@JsonProperty`
- [x] `CategoryResponse.products` (the category's associated `Product`s) on the detail endpoint only

### Configuration notes

- **`CategoryResponse.products` is only populated by `GET /api/v1/categories/{id}`, never by the paginated list.** `CategoryRepository.findByIdWithProducts` (`LEFT JOIN FETCH`) backs the detail endpoint; the list endpoint's `CategoryMapper.toResponse` explicitly sets `products` to an empty list rather than touching the entity's lazy, unfetched collection, avoiding an N+1 across a full page of categories.
- **Snake_case JSON via `@JsonProperty` per field, not a global Jackson naming strategy.** Keeps every Java field/record component camelCase (idiomatic, and MapStruct keeps matching DTO fields to entity fields by name automatically) while the public JSON contract is snake_case.
- **Flyway migrations on this branch are numbered `V3`, `V4` and `V13`, skipping `V2` and `V5-V12`.** `V2` is reserved for `feature/auth`; `V5-V12` are reserved for branches from `feature/auth` through `feature/user-management` that appear further up the merge chain. `V13` adds `description`, `active` and migrates `unit_price` from `REAL` to `NUMERIC(12,2)` on the products table.
- **`ProductServiceImpl.findAll` only falls back to a `Specification`-based query when `productName`/`minPrice`/`maxPrice` is supplied.** The plain and `categoryId`-only cases keep using the existing `@EntityGraph`-backed repository methods; the `Specification` path has no fetch join for `category`, an accepted N+1 risk kept simple rather than complicating `ProductSpecifications` with one, and scoped only to requests that actually use the new filters.
- **`minPrice`/`maxPrice` query parameters and `unitPrice` throughout the stack use `BigDecimal`.** `ProductSpecifications.priceBetween`, `ProductService.findAll`, `ProductController.findAll` and all DTOs have been updated from `Float`/`float` to `BigDecimal` to prevent rounding at the HTTP deserialization boundary.
- **`Product.active` is not writable from `ProductRequest`.** `ProductMapper.toEntity` and `updateEntityFromRequest` both ignore `active` to prevent a create/update call from accidentally clearing the flag. Activation and deactivation are managed separately.

## feature/customers

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/customers` | Paginated list, search by name (`?search=`) |
| GET | `/api/v1/customers/{id}` | Customer detail |
| POST | `/api/v1/customers` | Create a customer |
| PUT | `/api/v1/customers/{id}` | Update a customer |
| DELETE | `/api/v1/customers/{id}` | Delete a customer |

### Tasks

- [x] `Customer` entity
- [x] `CustomerRepository` with search (`findByEmail`, derived query on first/last name)
- [x] DTOs `CustomerRequest`/`CustomerResponse` with validation (valid email, phone format)
- [x] `CustomerMapper`
- [x] `CustomerService` interface and `CustomerServiceImpl` implementation (in `service/impl`): email uniqueness check on create and update
- [x] `CustomerController`
- [x] Unit and integration tests
- [x] `CustomerController` returns `ResponseEntity<ApiResponse<T>>`; `CustomerRequest`/`CustomerResponse` fields exposed in JSON as snake_case via `@JsonProperty`

### Configuration notes

- **`CustomerResponse` does not carry the customer's ordered products on this branch.** `Order` (and therefore any customer-to-product link) does not exist yet here; it is introduced by `feature/orders`, which is where that association becomes queryable and where this is implemented instead.

## feature/orders

### Model

- `Order` entity: `@ManyToOne` to `Customer` and `Product` (both nullable for historical compatibility), `@OneToMany(cascade = ALL, orphanRemoval = true)` to `OrderItem`, `@Enumerated(EnumType.STRING) OrderStatus status` (default `PENDING`), `BigDecimal totalAmount`, `String orderNumber` (unique, generated as `ORD-` + 8-char UUID prefix on create).
- `OrderItem` entity: product snapshot at order time (`productName`, `unitPrice` as `NUMERIC(12,2)`), `quantity`, `subtotal`; child of `Order` with `@ManyToOne(optional = false)` back-reference. Product snapshot decouples order history from future price changes.
- `OrderStatus` enum: `PENDING`, `CONFIRMED`, `CANCELLED`.
- Migration `V14__evolve_orders_domain.sql`: adds `order_number`, `status`, `total_amount` to `orders`; makes `customer_id`/`product_id` nullable; creates `order_items` table; backfills one `OrderItem` per historical order using the product snapshot; backfills `total_amount` from the legacy `total` column; generates `ORD-HIST-000001`-style order numbers for historical rows.
- Migration `V15__make_orders_total_legacy_nullable.sql`: drops the `NOT NULL` constraint on the legacy `total DOUBLE PRECISION` column, which V14 stopped mapping in the `Order` entity. The column is kept for schema-level backward compatibility but is no longer written by Hibernate.
- All monetary amounts are `BigDecimal` mapped to `NUMERIC(12,2)`. `totalAmount = unitPrice * quantity` computed in `OrderServiceImpl` using `BigDecimal.multiply`, never via floating-point arithmetic.

### Response shapes

- `OrderResponse` (one order) embeds `OrderCustomerResponse` and the full `ProductResponse`, plus `status` (String), `order_number`, `total_amount` (BigDecimal), and `items` (`List<OrderItemResponse>`). Two read paths: `findByIdWithDetails` (entity with `JOIN FETCH` on items, used for detail/create/update) and `findAllProjected` (17-field JPQL constructor-expression projection, used for the paginated list - no N+1).
- `CustomerResponse` carries `products` (one `OrderedProductResponse` per order) and `total` (BigDecimal sum). Loaded with a single `findAllProjectedByCustomerIds` (`IN` on the page's customer ids) for both the list and the detail.
- `GET /api/v1/orders` groups by customer (`CustomerOrdersResponse`), paginated via `findCustomersWithOrders`.
- `OrderController` returns `ResponseEntity<ApiResponse<T>>`; snake_case JSON fields via `@JsonProperty`.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/orders` | Orders grouped by customer (quantity and total per order, overall total). Paginated by customer, filterable by `customerId`/`productId` |
| GET | `/api/v1/orders/{id}` | Order detail with items |
| POST | `/api/v1/orders` | Create an order (`totalAmount` and `orderNumber` computed automatically) |
| PUT | `/api/v1/orders/{id}` | Update an order (replaces items, recomputes total) |
| DELETE | `/api/v1/orders/{id}` | Delete an order |
| GET | `/api/v1/customers/{id}/orders` | Orders for a given customer |

### Tasks

- [x] `OrderStatus` enum (`PENDING`, `CONFIRMED`, `CANCELLED`)
- [x] `OrderItem` entity with product snapshot (name + unit price at order time)
- [x] `Order` entity evolved: `totalAmount` (BigDecimal), `orderNumber`, `status`, `items` (cascade + orphanRemoval)
- [x] Migrations `V14` (multi-product schema + backfill) and `V15` (legacy `total` column nullable)
- [x] `OrderItemResponse` DTO, `OrderItemMapper`
- [x] `OrderResponse` updated with `status`, `total_amount`, `order_number`, `items`
- [x] `OrderCreatedEvent` enriched with `totalAmount` and `orderNumber`
- [x] `OrderRepository`: updated projections (17 fields), `findByIdWithDetails` with `JOIN FETCH` on items, `existsByCustomerId`, `existsByProductId`, `findByCustomerId`
- [x] `OrderServiceImpl`: `create`/`update` compute BigDecimal subtotal, generate `orderNumber`, build `OrderItem` before save
- [x] `OrderMapper`: `toResponse(Order)` with status enum-to-String expression; manual `toResponse(OrderProjection)` overload; `toOrderedProducts` helper; `OrderItemMapper` via `uses`
- [x] `CustomerResponse` returns the customer's orders and their total on both the list and the detail
- [x] Unit tests, repository tests (including `existsByCustomerId`, `existsByProductId`, `findByCustomerId`), controller tests, mapper tests

## feature/auth

### Model

- `AppUser` entity (id, username, email, hashed password, roles)
- `Role` enum (`ADMIN`, `USER`)
- Migration `V2__init_users_and_roles.sql`

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Sign up | Public |
| POST | `/api/v1/auth/login` | Sign in, returns a JWT | Public |
| GET | `/api/v1/auth/me` | Current user profile | Authenticated |

### Authorization rules

| Resource | GET | POST/PUT/DELETE |
|---|---|---|
| categories, products | Public | ADMIN |
| customers, orders | Authenticated | ADMIN |

### Tasks

- [x] `AppUser`, `Role`, `AppUserRepository`
- [x] `JwtService` (generation, validation, claims extraction)
- [x] `JwtAuthFilter` (`OncePerRequestFilter`)
- [x] `UserDetailsServiceImpl`
- [x] `SecurityConfig` (`SecurityFilterChain`, BCrypt `PasswordEncoder`, per-endpoint authorization rules, CSRF disabled for a stateless API, `STATELESS` session)
- [x] `AuthService` interface and `AuthServiceImpl` implementation (in `service/impl`), `AuthController` (register, login, me)
- [x] Custom 401/403 error handling (`AuthenticationEntryPoint`, `AccessDeniedHandler`)
- [x] Swagger updated to support the "Authorize" button (Bearer token)
- [x] Integration tests with `spring-security-test` (`@WithMockUser`, allowed/denied access tests)
- [x] `AuthController` returns `ResponseEntity<ApiResponse<T>>`; `AuthResponse.tokenType` exposed in JSON as `token_type` via `@JsonProperty` (the other auth DTO fields, `username`/`email`/`password`/`token`, are single words already identical in both conventions)

## feature/roles-permissions

Fine-grained access control: replaces the `Role` enum from `feature/auth` with a full JPA entity hierarchy (Role, Permission), CRUD endpoints for both, and method-level security via `@PreAuthorize` + a custom `PermissionEvaluator`.

### Model

- `Permission` entity (`id`, `resource`, `action`) - migration `V5`
- `Role` entity (`id`, `name`) with a `@ManyToMany` to `Permission`
- `role_permission` join table

### Endpoints

All endpoints are ADMIN-only (`@PreAuthorize("hasRole('ADMIN')")` at class level).

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/permissions` | Paginated list of permissions |
| POST | `/api/v1/permissions` | Create a permission |
| DELETE | `/api/v1/permissions/{id}` | Delete a permission |
| GET | `/api/v1/roles` | Paginated list of roles (with permissions) |
| POST | `/api/v1/roles` | Create a role |
| PUT | `/api/v1/roles/{id}` | Update a role name |
| DELETE | `/api/v1/roles/{id}` | Delete a role |
| PATCH | `/api/v1/roles/{id}/permissions` | Add and/or remove one or several permissions on a role, body `{"add": [1, 2], "remove": [3]}` |

### Tasks

- [x] `Permission` entity and `PermissionRepository`
- [x] `Role` entity (JPA, replaces enum) and `RoleRepository` with `@EntityGraph` on paginated `findAll` to avoid N+1 on permissions
- [x] Migration `V5__roles_permissions.sql`
- [x] DTOs `PermissionRequest`/`PermissionResponse`, `RoleRequest`/`RoleResponse`
- [x] `PermissionMapper`, `RoleMapper`
- [x] `PermissionService` / `PermissionServiceImpl` and `RoleService` / `RoleServiceImpl`
- [x] `PermissionController` and `RoleController` (ADMIN-only)
- [x] `MethodSecurityConfig` (`@EnableMethodSecurity`, static `MethodSecurityExpressionHandler` bean to avoid circular dependency)
- [x] `CustomPermissionEvaluator` (`PermissionEvaluator`): evaluates `hasPermission('RESOURCE', 'ACTION')` against `PERMISSION_RESOURCE:ACTION` authorities stored on the user
- [x] Service, repository, and controller tests

## feature/token-management

Token lifecycle management for account activation and password reset, plus async email delivery and scheduled cleanup.

### Model

- `ActivationToken` entity (`id`, `user`, `token`, `createdAt`, `expiresAt`, `validatedAt`) - migration `V6`; 24-hour TTL; `validatedAt` marks a used token (not deleted, auditable); `findByToken` carries `PESSIMISTIC_WRITE` lock to prevent double-use races
- `BlacklistedToken` entity (`id`, `user`, `token`, `jti`, `blacklistedAt`, `createdAt`, `expiresAt`, `validatedAt`) for JWT revocation; revocation check via `existsByJti` (no lock needed, read-only)
- `PasswordResetToken` entity (`id`, `user`, `token`, `type`, `expiryDate`) - 1-hour TTL; consumed by deletion (single-use: token is deleted before expiry is checked, so it is never reusable even if validation fails)

### Tasks

- [x] `ActivationToken`, `BlacklistedToken`, `PasswordResetToken` entities and repositories (package `entity/token`, `repository`)
- [x] Migration `V6__init_token_tables.sql` (activation_tokens, password_reset_tokens, blacklisted_tokens with FK to app_users, indexes on user_id)
- [x] `SecureTokenGenerator` (`@Component`, `SecureRandom`, 32 bytes Base64 URL-safe without padding)
- [x] `EmailService` interface and `EmailServiceImpl` (`@Async("emailTaskExecutor")` with `JavaMailSender`; uses executor from `AsyncConfig` in `feature/core-architecture`; `app.base-url` property for link generation)
- [x] `ActivationTokenService`/`ActivationTokenServiceImpl`, `BlacklistedTokenService`/`BlacklistedTokenServiceImpl`, `PasswordResetTokenService`/`PasswordResetTokenServiceImpl`
- [x] `TokenCleanupScheduler` (`@Scheduled(cron = "0 0 3 * * *")` daily at 3 AM; three separate `@Transactional` methods, one per table; uses `SchedulingConfig` from `feature/core-architecture`)
- [x] `InvalidTokenException` handler already present in `GlobalExceptionHandler` (wired by `feature/core-architecture`)
- [x] MailHog service added to `docker-compose.yml`; `application-dev.yml` points to `localhost:1025`; `application-prod.yml` reads `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` from env; `.env.example` updated with `MAIL_*` and `APP_BASE_URL`
- [x] Unit tests: `ActivationTokenServiceImplTest`, `BlacklistedTokenServiceImplTest`, `PasswordResetTokenServiceImplTest`, `EmailServiceImplTest`, `SecureTokenGeneratorTest`, `TokenCleanupSchedulerTest`
- [x] Repository tests (Testcontainers): `ActivationTokenRepositoryTest`, `BlacklistedTokenRepositoryTest`, `PasswordResetTokenRepositoryTest`
- [x] Fixed `@ActiveProfiles("test")` on all three `@SpringBootTest` full-context classes (`RepositoryAuditAspectTest`, `SpringBootTutorialApplicationTests`, `SecurityAuthorizationTest`) so `application-test.yml` (which stubs `spring.mail.host`) is loaded and `JavaMailSender` is auto-configured
- [x] `JwtService`: added `jti` claim (random UUID per token via `.id(UUID.randomUUID().toString())`), `extractJti(String token)`, and `extractExpiration(String token)`; removed `isTokenValid()` (replaced by `JwtException` in filter + blacklist check)
- [x] `AuthService.logout(String rawToken)` contract added to the interface
- [x] `AuthServiceImpl.logout()`: extracts jti/username/expiration from token, loads `AppUser`, delegates to `BlacklistedTokenService.blacklist()`
- [x] `AuthController`: `POST /api/v1/auth/logout` strips `Authorization: Bearer <token>` header and calls `authService.logout()`
- [x] `JwtAuthFilter`: injects `BlacklistedTokenService`; checks `isBlacklisted(jti)` before populating the `SecurityContext`; catches `DataAccessException` alongside `JwtException` for fail-open behaviour on DB outages
- [x] Tests: `JwtServiceTest` rewritten for jti model; `AuthServiceImplTest` extended with `@Mock BlacklistedTokenService` + two `logout()` tests; `AuthControllerTest` gains `_08_`/`_09_` for `POST /logout`; `SecurityAuthorizationTest._16_` exercises the full blacklist lifecycle (login -> GET 200 -> logout -> GET 401); all `@WebMvcTest` slices updated with `@MockitoBean BlacklistedTokenService`

## feature/user-management

Admin-facing user management: list, view, activate/deactivate accounts, assign roles; plus account activation and password-reset flows driven by email tokens.

### Model

- `AppUser` gains a `@ManyToMany` with `Role` (entity) via `role_user` join table, migration `V7`

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/users` | Paginated list of users | ADMIN |
| GET | `/api/v1/users/{id}` | User detail | ADMIN |
| PATCH | `/api/v1/users/{id}/roles` | Add and/or remove one or several roles on a user, body `{"add": [1, 2], "remove": [3]}` | ADMIN |
| DELETE | `/api/v1/users/{id}` | Delete a user | ADMIN |
| GET | `/api/v1/auth/activate` | Activate account with token | Public |
| POST | `/api/v1/auth/forgot-password` | Request a password-reset email | Public |
| POST | `/api/v1/auth/reset-password` | Reset password with token | Public |

### Tasks

- [x] Migration `V7__role_user.sql` (`role_user` join table, seeds ADMIN/USER roles, migrates enum data)
- [x] `AppUser` refactored from `@Enumerated Role` to `@ManyToMany Set<Role>` + `boolean enabled`
- [x] `UserDetailsServiceImpl` maps roles to `ROLE_` prefixed authorities, propagates `disabled(!enabled)`
- [x] `AppUserRepository.findByUsername` annotated `@EntityGraph(attributePaths="roles")` to avoid `LazyInitializationException` in `JwtAuthFilter`
- [x] `AuthServiceImpl.register()` sets `enabled=false`, loads USER role from DB, sends activation email
- [x] `UserController` (ADMIN-only, `@PreAuthorize` at class level)
- [x] Account activation flow (register -> activation email -> `GET /auth/activate?token=`)
- [x] Password-reset flow (forgot-password -> email -> `POST /auth/reset-password`)
- [x] `UserResponse` exposes `account_locked` field so the Angular users list can show lock status on initial load
- [x] Unit and integration tests (service, repository, controller, security authorization)

## feature/shopping-cart

Persistent shopping cart: one cart per authenticated user, lazily created on first add.

### Model

- `Cart` entity: `@OneToOne(LAZY)` to `AppUser` (unique, ON DELETE CASCADE), `@OneToMany(cascade=ALL, orphanRemoval=true)` to `CartItem`. Audited (`@CreatedDate`/`@LastModifiedDate`).
- `CartItem` entity: `@ManyToOne(LAZY)` to `Cart` and to `Product`. Quantity is a positive integer. The composite unique constraint `uq_cart_product(cart_id, product_id)` ensures each product appears at most once per cart.
- Migration `V17__create_cart_tables.sql`: creates `carts` and `cart_items` with FK and unique constraints and two indexes.
- `CartRepository` uses `@EntityGraph(attributePaths = {"items", "items.product"})` on both finders to load the full item graph in one query.

### Response shapes

- `CartItemResponse`: `id`, `product_id`, `product_name`, `unit_price`, `quantity`, `subtotal` (computed as `unitPrice * quantity`).
- `CartResponse`: `id`, `items`, `total` (sum of item subtotals).
- All monetary amounts are `BigDecimal`.

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/cart` | View the authenticated user's cart | USER |
| POST | `/api/v1/cart/items` | Add a product to the cart (increments quantity if already present) | USER |
| PATCH | `/api/v1/cart/items/{itemId}` | Update the quantity of a cart item | USER |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove one item from the cart | USER |
| DELETE | `/api/v1/cart/items` | Clear the entire cart | USER |

### Business rules

- Only active products (`active = true`) may be added to the cart (422 if inactive).
- Adding the same product twice increments the existing item's quantity rather than creating a duplicate row.
- `updateItem` and `removeItem` verify the item belongs to the caller's cart (404 otherwise).
- The user is always resolved from `@AuthenticationPrincipal UserDetails`, never from the payload.

### Tasks

- [x] Migration `V17__create_cart_tables.sql`
- [x] `Cart` and `CartItem` entities
- [x] `CartItemRequest`, `CartItemUpdateRequest`, `CartItemResponse`, `CartResponse` DTOs
- [x] `CartMapper` with computed `subtotal` per item and `total` for the cart
- [x] `CartRepository` and `CartItemRepository`
- [x] `CartService` interface and `CartServiceImpl`
- [x] `CartController` with 5 endpoints at `/api/v1/cart`
- [x] `SecurityConfig` updated: `/api/v1/cart/**` requires `ROLE_USER`
- [x] Unit tests (`CartServiceImplTest`), controller tests (`CartControllerTest`), repository tests (`CartRepositoryTest`)

## feature/order-checkout

Converts the authenticated user's cart into a confirmed order in a single atomic transaction.

### Checkout flow

1. `POST /api/v1/orders` (no body) is called by an authenticated user with `ROLE_USER`.
2. The service loads the user's cart (404 if none exists, 422 if empty).
3. For each `CartItem`, the product is **re-fetched from the database** to obtain the current price. The service never trusts any price stored in the cart or supplied by the client.
4. Each item is validated: 422 if the product is inactive.
5. An `Order` is created with one `OrderItem` snapshot per cart line. The `Order.user` FK links to the authenticated `AppUser` (no `Customer` record is required).
6. `totalAmount` is computed server-side from the re-fetched prices.
7. The cart items are deleted **in the same transaction**. If the `Order` save fails and rolls back, the cart is left untouched.
8. `OrderCreatedEvent` is published after the transaction commits.

### Model additions

- `Order` gains a nullable `user_id` FK to `app_users` (migration `V18`). Legacy orders created via the old CRUD endpoint keep `customer_id` only; checkout orders set `user_id` only.
- `CheckoutRequest` is an intentionally empty record: the request body carries no data.

### Endpoint

| Method | URL | Description | Access |
|---|---|---|---|
| POST | `/api/v1/orders` | Checkout: convert cart to order | USER |

The `PUT /api/v1/orders/{id}` and `DELETE /api/v1/orders/{id}` endpoints remain for ADMIN-only legacy CRUD.

### Security rules

- `POST /api/v1/orders` requires `ROLE_USER` (changed from ADMIN-only).
- `PUT` and `DELETE` on orders still require `ROLE_ADMIN`.
- The user is always resolved from `@AuthenticationPrincipal UserDetails`, never from an HTTP parameter.

### Tasks

- [x] Migration `V18__add_user_id_to_orders.sql`
- [x] `AppUser user` field added to `Order` entity
- [x] `CheckoutRequest` DTO
- [x] `checkout(String username)` added to `OrderService` interface
- [x] `checkout` implemented in `OrderServiceImpl` with atomic cart-to-order conversion
- [x] `OrderController.checkout()` replaces `create(@RequestBody OrderRequest)`
- [x] `deleteAllByCartId` added to `CartItemRepository`
- [x] `SecurityConfig` updated: `POST /api/v1/orders` now requires `ROLE_USER`
- [x] Unit tests (`OrderServiceImplTest` +5 checkout cases), controller tests (`OrderControllerTest` updated)

## feature/order-notifications

Post-commit email notifications sent after a successful checkout. The order
is already durable when these emails are sent; an SMTP failure logs an error
but never affects the order or the HTTP response returned to the client.

### How it works

1. `OrderServiceImpl.checkout()` publishes `OrderCreatedEvent` after saving the order.
2. `OrderCreatedEventListener.onOrderCreated()` is annotated with both
   `@TransactionalEventListener(AFTER_COMMIT)` and `@Async("emailTaskExecutor")`:
   - `AFTER_COMMIT` ensures the event only fires once the row is durable.
   - `@Async` moves the work to the email thread pool, decoupling SMTP latency
     from the event dispatch thread.
3. The listener reloads the full order (items + `AppUser`) from the database using
   `OrderRepository.findByIdWithItemsAndUser`. Re-loading is necessary because the
   original transaction is closed and accessing lazy associations on a detached
   entity would raise `LazyInitializationException`.
4. `OrderEmailServiceImpl.sendOrderConfirmation` sends a plain-text confirmation to
   the customer's email address (skipped when the order has no `AppUser`).
5. `OrderEmailServiceImpl.sendAdminNotification` sends a plain-text alert to the
   configured admin address.

### Configuration

| Property | Env variable | Default |
|---|---|---|
| `app.notifications.admin-email` | `APP_NOTIFICATION_ADMIN_EMAIL` | `admin@example.com` |
| `app.mail.from` | `MAIL_FROM` | `noreply@spring-boot-tutorial.com` |

#### MailHog in dev (docker-compose)

The `docker-compose.yml` runs MailHog on port 1025 (SMTP) and 8025 (web UI).
Point a browser at `http://localhost:8025` to see captured emails.

#### Behaviour on SMTP failure

`MailException` is caught inside each email method and logged at `ERROR` level.
The exception is never re-thrown. The order is unaffected by an email failure.

### New files

- `NotificationProperties` - `@ConfigurationProperties(prefix = "app.notifications")`
- `OrderEmailService` - contract for the two email methods
- `OrderEmailServiceImpl` - `@Async("emailTaskExecutor")`, plain-text bodies

### Tasks

- [x] `NotificationProperties` with `adminEmail` field
- [x] `@EnableConfigurationProperties(NotificationProperties.class)` registered in `AsyncConfig`
- [x] `app.notifications.admin-email` added to `application.yml` and `.env.example`
- [x] `OrderEmailService` interface
- [x] `OrderEmailServiceImpl` with SMTP error handling
- [x] `findByIdWithItemsAndUser` added to `OrderRepository` (LEFT JOIN FETCH, works for checkout and legacy orders)
- [x] `OrderCreatedEventListener` extended with `@Async` + email calls
- [x] `OrderEmailServiceImplTest` (3 cases: confirmation, admin notification, SMTP exception swallowed)
- [x] `OrderCreatedEventListenerTest` (2 cases: with user, without user)

## feature/order-authorization

Role-based access control on order endpoints and status lifecycle management.
A `ROLE_USER` sees only their own orders; a `ROLE_ADMIN` sees all orders and
can update statuses. Ownership checks live in the service layer, never only in
the controller.

### Authorization matrix

| Endpoint | ROLE_USER | ROLE_ADMIN | Unauthenticated |
|---|---|---|---|
| `GET /api/v1/orders` | Own orders only | All orders | 401 |
| `GET /api/v1/orders/{id}` | Own order or 404 | Any order | 401 |
| `POST /api/v1/orders` (checkout) | Allowed | Allowed | 401 |
| `PATCH /api/v1/orders/{id}/status` | 403 | Allowed | 401 |
| `PUT /api/v1/orders/{id}` | 403 | Allowed | 401 |
| `DELETE /api/v1/orders/{id}` | 403 | Allowed | 401 |
| `GET /api/v1/cart/**` | Allowed | 403 | 401 |

### 404 vs 403 on order ownership

When a `ROLE_USER` requests an order that belongs to another user, the API
returns **404**, not 403. Returning 403 would reveal that the resource exists;
404 is the correct response to avoid leaking ownership information.

### Status transition rules

Only a `ROLE_ADMIN` may change an order's status. Allowed transitions:

| From | To | Result |
|---|---|---|
| `PENDING` | `CONFIRMED` | 200 |
| `PENDING` | `CANCELLED` | 200 |
| `CONFIRMED` | `CANCELLED` | 200 |
| Any other pair | - | 422 Unprocessable Entity |

### New files

- `OrderStatusUpdateRequest` - DTO for `PATCH /{id}/status`, carries `@NotNull OrderStatus`
- `OrderAuthorizationTest` - Testcontainers integration test with 3 actors (userA, userB, admin)

### Tasks

- [x] `OrderStatusUpdateRequest` DTO
- [x] `findByUserId` (EntityGraph on items + items.product) in `OrderRepository`
- [x] `findByIdAndUserId` (JOIN FETCH items, filters by user) in `OrderRepository`
- [x] `OrderService.findAll(String username, boolean isAdmin, Pageable)` replaces old `findAll(Long, Long, Pageable)`
- [x] `OrderService.findById(Long id, String username, boolean isAdmin)` replaces old `findById(Long)`
- [x] `OrderService.updateStatus(Long id, OrderStatus)` with transition validation
- [x] `OrderServiceImpl` authorization logic (empty page on user-not-found for list)
- [x] `OrderController` rewritten with `@AuthenticationPrincipal`, `@PreAuthorize` on status/update/delete
- [x] `SecurityConfig` updated with explicit GET/POST/PATCH/PUT/DELETE rules for `/api/v1/orders/**`
- [x] `OrderControllerTest` updated (12 cases including `updateStatus` happy/error paths)
- [x] `OrderAuthorizationTest` (8 cases: ownership, admin access, status transitions, 401)

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/products` (depends on `core-architecture`) → Pull Request to `develop`
3. `feature/customers` → Pull Request to `develop`
4. `feature/orders` (depends on `products` and `customers`) → Pull Request to `develop`
5. `feature/auth` (secures everything) → Pull Request to `develop`
6. `feature/roles-permissions` (fine-grained RBAC) → Pull Request to `develop`
7. `feature/token-management` (token lifecycle, async email, scheduled cleanup) → Pull Request to `develop`
8. `feature/user-management` (admin user CRUD, activation/reset flows) → Pull Request to `develop`
9. `feature/product-images` (from `feature/products` HEAD — independent of the auth chain) — no PR, merged into `feature/integration` only
10. `feature/shopping-cart` (from `feature/user-management`, merges `feature/orders`) — no PR, merged into next branch
11. `feature/order-checkout` (from `feature/shopping-cart`) — no PR, merged into next branch
12. `feature/order-notifications` (from `feature/order-checkout`) — no PR, merged into next branch
13. `feature/order-authorization` (from `feature/order-notifications`) — no PR, merged into next branch
14. `feature/integration` (from `feature/order-authorization`, merges `feature/product-images`) → **single Pull Request to `develop`** for the entire e-commerce chain
15. `develop` → `master` once everything is tested and validated

## Code conventions

- Root package: `com.edgareldy.springboottutorial`
- DTOs: Java `record` rather than classes (immutability, less boilerplate)
- No business logic in controllers: delegate to the service layer only
- **Contract/implementation services**: the interface (`XxxService`) lives at the root of `service/`, its implementation (`XxxServiceImpl`) lives in `service/impl/`. Controllers and tests only depend on the interface (injected by interface type), never directly on the implementation.
- Any service method that writes to the database is annotated `@Transactional` (on the implementation)
- Every controller returns a `ResponseEntity<ApiResponse<T>>` (see [Standard response format](#standard-response-format)), including `GlobalExceptionHandler` for errors
- DTO fields are exposed in JSON as snake_case via `@JsonProperty` on each record component, kept camelCase in Java so mappers and services stay idiomatic
- List responses are always paginated (`Page<T>` → `PageResponse<T>` DTO)
- Endpoint names are plural, `kebab-case` when composed
- Every endpoint documented with `@Operation(summary = "...")`
- All REST endpoints are versioned under the `/api/v1` prefix (e.g. `/api/v1/categories`), so the API can introduce a breaking `/api/v2` later without touching existing clients
- **Test naming convention**: every test method is named `_NN_Should<Outcome>_When<Condition>`, where `NN` is a two-digit sequence number with a leading zero, restarting at `_01_` in each test class and following the order of the methods in the source (e.g. `_01_ShouldReturnCategory_WhenCategoryExists`, `_02_ShouldReturnEmpty_WhenCategoryDoesNotExist`). This applies to every kind of test (unit, repository, controller, integration, parameterized). No other style (`shouldX()`, `testX()`, `givenX_whenY_thenZ()`) is accepted.

## Concepts covered

- Layered architecture (controller / service / repository)
- Spring Data JPA: relations, derived queries, `@Query`, pagination, sorting, projections
- DTOs and mapping (MapStruct)
- Validation (Bean Validation)
- Centralized exception handling (`ProblemDetail`, RFC 7807)
- Transactions (`@Transactional`)
- Optimistic locking (`@Version`)
- Dynamic queries (`Specification`/`JpaSpecificationExecutor`)
- Native JPA auditing (`@CreatedDate`/`@LastModifiedDate`)
- Schema migrations (Flyway)
- API documentation (OpenAPI / Swagger UI)
- Observability (Actuator, including a custom endpoint; structured logging)
- Security (Spring Security, JWT, role-based authorization)
- Method-level security (`@PreAuthorize`, `@EnableMethodSecurity`, custom `PermissionEvaluator`)
- Async processing (`@Async`, `@EnableAsync`, `ThreadPoolTaskExecutor`)
- Scheduled tasks (`@Scheduled`, `@EnableScheduling`)
- Email sending (`JavaMailSender`, MailHog for local testing)
- Testing (unit, integration, Testcontainers)
- Multi-environment configuration (Spring profiles)
- `@ConfigurationProperties`
- Application cache (`@Cacheable`)
- Data seeding (`CommandLineRunner`)
- Application events (`ApplicationEventPublisher`)
- Aspect-oriented programming (Spring AOP: `@Around`, `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`)
- Local development tooling (Spring Boot DevTools)
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions)
- File upload (`MultipartFile`, `FileStorageService`, disk storage with configurable path)
- Persistent shopping cart (entity-backed cart tied to authenticated user)
- Atomic checkout (multi-entity transactional write: cart drain + order creation)
- Role-based data ownership (`@AuthenticationPrincipal`, users see only their own orders)

## How to follow this tutorial

1. Clone the repository and check out `develop`
2. Create/checkout the `feature/core-architecture` branch and follow its task checklist
3. Continue with `feature/products`, `feature/customers`, `feature/orders`, `feature/auth`, `feature/roles-permissions`, `feature/token-management`, `feature/user-management`, `feature/product-images`, `feature/shopping-cart`, `feature/order-checkout`, `feature/order-notifications`, `feature/order-authorization`, `feature/integration` in that order
4. Open a Pull Request to `develop` at the end of each branch
5. Copy `.env.example` to `.env` and fill in the required variables
6. Run the project with `docker-compose up`, then open Swagger UI at `http://localhost:8080/swagger-ui.html`
7. MailHog UI is available at `http://localhost:8025` to inspect outgoing emails in local dev

## feature/product-images

Adds image management to the product catalogue. Created from `feature/products` HEAD, independent of the main authentication/user chain.

### Endpoints

| Method | URL | Description |
|---|---|---|
| POST | `/api/v1/products/{id}/images` | Upload an image for a product (multipart/form-data, ROLE_ADMIN) |
| DELETE | `/api/v1/products/{id}/images/{imageId}` | Delete a product image (ROLE_ADMIN) |

### Tasks

- [ ] `ProductImage` entity (`product_id`, `filename`, `url`, `sort_order`)
- [ ] Flyway migration V15
- [ ] `FileStorageService` interface + `LocalFileStorageService` impl (stores files under `${app.storage.upload-dir}`)
- [ ] `StorageProperties` record (`@ConfigurationProperties(prefix = "app.storage")`, `uploadDir`, `maxFileSizeMb`)
- [ ] `POST /api/v1/products/{id}/images`: receives `MultipartFile`, delegates to `FileStorageService`, persists `ProductImage`
- [ ] `DELETE /api/v1/products/{id}/images/{imageId}`: deletes record and removes file from disk
- [ ] `ProductResponse` gains `List<ProductImageResponse> images` (always populated)
- [ ] Unit tests for `FileStorageService` (happy path, size limit, unsupported type)
- [ ] MockMvc controller tests

### Configuration notes

- **Branch is independent of the auth chain** (`feature/auth` through `feature/user-management`). It is merged into `feature/integration` at the end, not cascaded through the main chain.
- **`StorageProperties` is a `record` with `@ConfigurationProperties`**, consistent with `JwtProperties` from `feature/auth`.
- **File type validation** rejects anything that is not `image/jpeg`, `image/png` or `image/webp`.

## feature/shopping-cart

Persistent shopping cart backed by JPA entities, one cart per authenticated user.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/cart` | Get the current user's cart (creates it if absent) |
| POST | `/api/v1/cart/items` | Add a product to the cart |
| PATCH | `/api/v1/cart/items/{itemId}` | Update item quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove a single item |
| DELETE | `/api/v1/cart` | Clear the entire cart |

### Tasks

- [ ] `Cart` entity (`user_id` FK → `app_users`, timestamps via `@CreatedDate`/`@LastModifiedDate`)
- [ ] `CartItem` entity (`cart_id`, `product_id`, `quantity`)
- [ ] Flyway migration V16
- [ ] `CartService` interface + `CartServiceImpl`
- [ ] All endpoints require ROLE_USER; user resolved via `@AuthenticationPrincipal UserDetails`
- [ ] `CartResponse` DTO: `id`, `List<CartItemResponse> items`, `total` (sum computed server-side)
- [ ] `AddCartItemRequest` / `UpdateCartItemRequest` DTOs with validation
- [ ] Unit tests (Mockito) + MockMvc controller tests

### Configuration notes

- **Cart is created lazily** on first `GET /api/v1/cart` for a user who has none.
- **`total` is computed by the service layer** from live product prices, never stored.
- Branch starts from `feature/user-management` (needs `AppUser`) and merges `feature/orders` (needs `Order` schema context for checkout).

## feature/order-checkout

Atomic cart-to-order conversion: a single `POST /api/v1/orders` drains the cart and creates a fully populated `Order` with `OrderItem` lines.

### Endpoints

| Method | URL | Description |
|---|---|---|
| POST | `/api/v1/orders` | Checkout: convert cart to order (ROLE_USER) |
| GET | `/api/v1/orders` | List the current user's orders, paginated |
| GET | `/api/v1/orders/{id}` | Order detail with items |

### Tasks

- [ ] `OrderItem` entity (`order_id`, `product_id`, `quantity`, `unit_price` snapshot)
- [ ] `Order` refactor: add `user_id`, `order_number` (UUID), `status` enum (PENDING/CONFIRMED/SHIPPED/DELIVERED/CANCELLED), `total_amount NUMERIC(12,2)`
- [ ] Flyway migration V14 (new columns, `order_items` table, historical data migration)
- [ ] `CheckoutService` or `OrderService.checkout(UserDetails)`: `@Transactional`, loads cart, creates `Order` + `OrderItem` lines (price snapshot), clears cart
- [ ] `CheckoutRequest` DTO (optional `notes` field)
- [ ] `OrderResponse` DTO: `id`, `orderNumber`, `status`, `totalAmount`, `List<OrderItemResponse> items`, `createdAt`
- [ ] Service unit tests (atomic write, empty-cart 422, out-of-stock 422)
- [ ] MockMvc controller tests

### Configuration notes

- **Price snapshot**: `OrderItem.unitPrice` captures `Product.unitPrice` at checkout time so future price changes do not alter historical orders.
- **Empty cart returns 422** (`BusinessRuleException`), not 400.
- **`order_number` is a UUID** generated by the service, never supplied by the client.

## feature/order-notifications

Sends an email to the user and to the admin when an order is placed, using the existing Spring Mail infrastructure (`JavaMailSender`, MailHog on dev).

### Tasks

- [ ] `NotificationProperties` record (`@ConfigurationProperties(prefix = "app.notifications")`, `adminEmail`)
- [ ] `OrderEmailService` interface + `OrderEmailServiceImpl`: `sendOrderConfirmationToUser(Order)` and `sendOrderAlertToAdmin(Order)`, both `@Async("emailTaskExecutor")`
- [ ] `OrderCreatedEventListener` updated to call `OrderEmailService` after commit (`@TransactionalEventListener(AFTER_COMMIT)`)
- [ ] Email templates (plain text, inline in the service or as `src/main/resources/templates/*.txt`)
- [ ] Unit tests (mock `JavaMailSender`, verify `MimeMessage` recipients)

### Configuration notes

- **Same async executor as `feature/token-management`** (`emailTaskExecutor` defined in `AsyncConfig`).
- **`NotificationProperties.adminEmail`** is supplied via `APP_NOTIFICATION_ADMIN_EMAIL` env var; added to `.env.example`.

## feature/order-authorization

Restricts order visibility and status mutation by role.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/orders` | ROLE_USER sees only their own orders; ROLE_ADMIN sees all |
| GET | `/api/v1/orders/{id}` | Returns 404 (not 403) if the order belongs to another user |
| PATCH | `/api/v1/orders/{id}/status` | Update order status (ROLE_ADMIN only) |

### Tasks

- [ ] `SecurityConfig` rules: `GET /api/v1/orders/**` → authenticated; ownership enforced in service
- [ ] `OrderService.findAll(UserDetails, Pageable)` and `findById(Long, UserDetails)`: admin bypasses ownership check, user gets 404 on others' orders
- [ ] `PATCH /api/v1/orders/{id}/status` → `@PreAuthorize("hasRole('ADMIN')")`, body `OrderStatusUpdateRequest` with `@NotNull OrderStatus status`
- [ ] `OrderStatusUpdateRequest` DTO
- [ ] `@SpringBootTest` authorization tests: user isolation, admin override, status update access

### Configuration notes

- **404 instead of 403 on ownership mismatch**: returning 403 leaks the existence of the resource; a consistent 404 is safer.
- **Status transitions are not validated** (any admin can set any status): validation logic can be added later without breaking the API.

## feature/integration

Final integration branch: merges `feature/product-images` into the main chain and validates the complete e-commerce flow end-to-end.

### Tasks

- [ ] Merge `feature/product-images` (resolve conflicts on `ProductResponse`, `ProductController`, `application.yml`)
- [ ] `EcommerceIntegrationTest` (`@SpringBootTest` + Testcontainers): register, login, browse catalogue, upload image (admin), add items to cart, checkout, verify order, admin status update, user isolation (user A cannot see user B's order)
- [ ] `docker-compose.yml` updated: volume `./uploads:/app/uploads`, env vars `APP_STORAGE_UPLOAD_DIR`, `APP_NOTIFICATION_ADMIN_EMAIL`
- [ ] `.env.example` updated with all new variables
- [ ] README updated with the full e-commerce endpoint table and configuration instructions

### Configuration notes

- **This is the only PR to `develop`** for the entire e-commerce extension chain (phases 3–8); the intermediate branches (`shopping-cart` through `order-authorization`) are merged feature-to-feature.
- **`feature/product-images` is the only side branch** and is merged here rather than through the main chain.
