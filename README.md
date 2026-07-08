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
| Tests | JUnit 5, Mockito, Testcontainers, AssertJ |
| CI/CD | GitHub Actions |
| Containerization | Docker, docker-compose |

## Data model

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
| unit_price | FLOAT | NOT NULL, > 0 |

**customers**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| first_name | VARCHAR(255) | NOT NULL |
| last_name | VARCHAR(255) | NOT NULL |
| telephone | VARCHAR(50) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE, valid email format |
| address | VARCHAR(255) | NOT NULL |

**orders**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| customer_id | BIGINT | FK → customers.id, NOT NULL |
| product_id | BIGINT | FK → products.id, NOT NULL |
| quantity | INT | NOT NULL, > 0 |
| total | DOUBLE | NOT NULL, computed = quantity × product unit_price |

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

Each feature is developed on its own branch, then merged into `develop` via a documented **Pull Request** (even solo), to keep a clear, educational trace of each step.

## Project structure

```
spring-boot-tutorial/
├── src/
│   ├── main/
│   │   ├── java/edgareldy/springboottutorial/
│   │   │   ├── SpringBootTutorialApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── CacheConfig.java
│   │   │   ├── entity/
│   │   │   │   ├── Category.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Customer.java
│   │   │   │   ├── Order.java
│   │   │   │   └── user/
│   │   │   │       ├── AppUser.java
│   │   │   │       └── Role.java
│   │   │   ├── repository/
│   │   │   │   ├── CategoryRepository.java
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── CustomerRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── AppUserRepository.java
│   │   │   ├── dto/
│   │   │   │   ├── common/
│   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   └── PageResponse.java
│   │   │   │   ├── category/ (CategoryRequest, CategoryResponse)
│   │   │   │   ├── product/ (ProductRequest, ProductResponse)
│   │   │   │   ├── customer/ (CustomerRequest, CustomerResponse)
│   │   │   │   ├── order/ (OrderRequest, OrderResponse)
│   │   │   │   └── auth/ (RegisterRequest, LoginRequest, AuthResponse)
│   │   │   ├── mapper/
│   │   │   │   ├── CategoryMapper.java
│   │   │   │   ├── ProductMapper.java
│   │   │   │   ├── CustomerMapper.java
│   │   │   │   └── OrderMapper.java
│   │   │   ├── service/
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── CustomerService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   └── impl/
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── ProductServiceImpl.java
│   │   │   │       ├── CustomerServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       └── AuthServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── CustomerController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── AuthController.java
│   │   │   ├── exception/
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── event/
│   │   │   │   ├── OrderCreatedEvent.java
│   │   │   │   └── OrderCreatedEventListener.java
│   │   │   └── aspect/
│   │   │       ├── LoggingAspect.java
│   │   │       └── ExecutionTimeAspect.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__init_schema.sql
│   │           └── V2__init_users_and_roles.sql
│   └── test/
│       └── java/edgareldy/springboottutorial/
│           ├── controller/ (MockMvc tests)
│           ├── service/ (Mockito unit tests)
│           └── repository/ (@DataJpaTest / Testcontainers)
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
- On error, `GlobalExceptionHandler` returns an `ApiResponse<Void>` with `success=false`, an explicit `message`, and structured details (`ErrorResponse`) placed in `data` when relevant (field validation, etc.).
- Success response example:

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { "id": 12, "productName": "Mechanical keyboard", "unitPrice": 79.99, "categoryId": 3 },
  "timestamp": "2026-07-02T10:15:30Z"
}
```

- Error response example:

```json
{
  "success": false,
  "message": "Product not found",
  "data": null,
  "timestamp": "2026-07-02T10:16:05Z"
}
```

## Spring AOP

The project uses **Spring AOP** to illustrate aspect-oriented programming, kept out of any business logic in services/controllers.

- Dependency: `spring-boot-starter-aop`
- `LoggingAspect` (`aspect/LoggingAspect.java`): `@Around` advice on all `@Service` beans (pointcut `execution(* edgareldy.springboottutorial.service..*(..))`), logs method entry/exit, arguments, and thrown exceptions
- `ExecutionTimeAspect` (`aspect/ExecutionTimeAspect.java`): `@Around` advice on controller methods, measures and logs the execution time of each HTTP request
- `RepositoryAuditAspect` (`aspect/RepositoryAuditAspect.java`): on the repository layer, the four advice types the other two aspects don't use: `@Before`/`@After` around every repository call, `@AfterReturning` on `save` specifically, `@AfterThrowing` on any repository exception
- Can later be reused for auditing (e.g. tracing who created/modified an order) without polluting business code

## feature/core-architecture

Technical foundation shared by the whole project, to be merged first into `develop`. Originally named `feature/config`; renamed to better reflect that it lays out the whole architectural skeleton (config, cross-cutting concerns, containerization, CI), not just configuration files.

### Tasks

- [x] Initialize the project via Spring Initializr (Maven, Java 17, Spring Boot 3.5.16)
- [x] Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-aop`, `flyway-core`, `flyway-database-postgresql`, `postgresql` driver, `lombok`, `mapstruct` + `mapstruct-processor`, `springdoc-openapi-starter-webmvc-ui`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`
- [x] Test dependencies: `spring-boot-starter-test`, `spring-security-test`, `testcontainers` (junit-jupiter, postgresql)
- [x] Create the package tree shown above
- [x] `application.yml`: shared configuration (app name, port, JSON date format)
- [x] `application-dev.yml`: local datasource, `spring.jpa.show-sql=true`, Flyway enabled
- [x] `application-test.yml`: Testcontainers datasource
- [x] `application-prod.yml`: datasource via environment variables, JSON logs
- [x] Flyway script `V1__init_schema.sql` (categories, products, customers, orders tables with FKs)
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`): handles `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400, field details), `BusinessRuleException` (422), generic `Exception` (500)
- [x] Standard `ErrorResponse` DTO: `timestamp`, `status`, `error`, `message`, `path`, optional `fieldErrors` list
- [x] Generic `ApiResponse<T>` (`dto/common/ApiResponse.java`) and `PageResponse<T>` (`dto/common/PageResponse.java`) DTOs, used to wrap every controller response
- [x] `LoggingAspect` and `ExecutionTimeAspect` (`aspect/`): logging and execution-time measurement via Spring AOP (see [Spring AOP](#spring-aop))
- [x] `OpenApiConfig`: title, description, version, JWT security scheme in Swagger UI
- [x] Actuator: expose `health`, `info`, `metrics` in dev; `health` only in prod
- [x] Structured logging (`logback-spring.xml` and Actuator ECS/JSON config, combined)
- [x] `CorsConfig`: allow `localhost:4200` in dev
- [x] Multi-stage `Dockerfile` (Maven build + lightweight JRE image)
- [x] `docker-compose.yml`: `app` service + `db` service (PostgreSQL 16) with volumes and environment variables
- [x] `.github/workflows/ci.yml`: Maven build + tests on every push/PR
- [x] Branch `README` explaining the configuration choices (see below)

### Configuration notes

- **Spring Boot 3.5.16, not 4.1.x.** The project was first scoped around Spring Boot 4.1 / Spring Framework 7, but springdoc-openapi had no release compatible with Spring Framework 7 on Maven Central at implementation time. Spring Boot 3.5.16 (Spring Framework 6) is fully compatible with every required dependency (springdoc, MapStruct, jjwt) and is still an actively maintained line, so it was chosen instead.
- **`dependencyManagement` explicitly imports `spring-boot-dependencies`** as `${project.parent.version}`, in addition to inheriting it via `<parent>`. This is redundant (the parent POM already provides the same bill of materials) but was requested explicitly to make version management visible directly in `dependencyManagement`.
- **`hibernate.ddl-auto` is `validate` in every profile**, never `update` or `create`. Flyway's `V1__init_schema.sql` is the single source of truth for the schema; Hibernate only checks that entity mappings agree with it. This avoids the classic drift where a forgotten entity annotation silently alters the schema in one environment but not another.
- **Structured logging combines two mechanisms on purpose.** `logback-spring.xml` includes Spring Boot's own default appenders (`defaults.xml`, `console-appender.xml`) instead of redefining them, and adds a prod-only `file-appender.xml` include via `<springProfile name="prod">`. The actual JSON formatting comes from Spring Boot 3.4+'s native `logging.structured.format.console`/`.file` properties (set to `ecs` in `application-prod.yml`), so no extra dependency (e.g. `logstash-logback-encoder`) or hand-written JSON encoder was needed.
- **`CorsConfig` is annotated `@Profile("dev")`.** Only the local Angular dev server (`localhost:4200`) gets a CORS exemption; prod is expected to declare its own, narrower policy once a real frontend origin exists.
- **Testcontainers only, no H2.** `application-test.yml` declares no datasource at all: `TestcontainersConfiguration`'s `@ServiceConnection` `PostgreSQLContainer` bean wires the datasource automatically. Using real PostgreSQL in tests (instead of an in-memory H2 database) avoids behavioral differences between the two engines (types, constraints, SQL dialect) that would otherwise only surface in production.

## feature/products

Includes `Category` and `Product`, given their direct link in the model.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/categories` | Paginated list of categories |
| GET | `/api/v1/categories/{id}` | Category detail |
| POST | `/api/v1/categories` | Create a category |
| PUT | `/api/v1/categories/{id}` | Update a category |
| DELETE | `/api/v1/categories/{id}` | Delete a category |
| GET | `/api/v1/products` | Paginated list, filterable by `categoryId`, sortable by `productName`/`unitPrice` |
| GET | `/api/v1/products/{id}` | Product detail |
| POST | `/api/v1/products` | Create a product |
| PUT | `/api/v1/products/{id}` | Update a product |
| DELETE | `/api/v1/products/{id}` | Delete a product |

### Tasks

- [ ] `Category`, `Product` entities with `@OneToMany`/`@ManyToOne` relation (`LAZY` loading)
- [ ] `CategoryRepository`, `ProductRepository` (Spring Data JPA) with derived queries (`findByCategoryId`) and one `@Query` using `JOIN FETCH`
- [ ] DTOs `CategoryRequest`/`CategoryResponse`, `ProductRequest`/`ProductResponse`
- [ ] Corresponding MapStruct mappers
- [ ] `CategoryService`, `ProductService` interfaces (contracts) and their `CategoryServiceImpl`, `ProductServiceImpl` implementations in `service/impl`, annotated `@Service`, with `@Transactional` on writes
- [ ] Business rule: a category containing products cannot be deleted (`BusinessRuleException`)
- [ ] `CategoryController`, `ProductController`: REST CRUD + pagination (`Pageable`) + sorting
- [ ] OpenAPI documentation (`@Operation`, `@ApiResponse`) on every endpoint
- [ ] Service unit tests (Mockito)
- [ ] `@DataJpaTest` repository tests
- [ ] MockMvc controller integration tests

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

## feature/orders

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/orders` | Paginated list, filterable by `customerId`/`productId` |
| GET | `/api/v1/orders/{id}` | Order detail |
| POST | `/api/v1/orders` | Create an order (`total` computed automatically) |
| PUT | `/api/v1/orders/{id}` | Update an order |
| DELETE | `/api/v1/orders/{id}` | Delete an order |
| GET | `/api/v1/customers/{id}/orders` | Orders for a given customer |

### Tasks

- [x] `Order` entity with `@ManyToOne` to `Customer` and `Product`
- [x] `OrderRepository` with join queries (`@Query` with `JOIN`), DTO projection for lists
- [x] DTOs `OrderRequest` (customerId, productId, quantity) / `OrderResponse` (with summarized customer/product sub-objects)
- [x] `OrderMapper`
- [x] `OrderService` interface and `OrderServiceImpl` implementation (in `service/impl`): computes `total = quantity * product.unitPrice`, checks that the customer and product exist
- [x] `OrderCreatedEvent` application event published after creation, consumed by `OrderCreatedEventListener` (e.g. business logging, future email notification)
- [x] `OrderController`
- [x] Unit tests (total computation, business rules) and integration tests

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

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/products` (depends on `config`) → Pull Request to `develop`
3. `feature/customers` → Pull Request to `develop`
4. `feature/orders` (depends on `products` and `customers`) → Pull Request to `develop`
5. `feature/auth` (secures everything) → Pull Request to `develop`
6. `develop` → `master` once everything is tested and validated

## Code conventions

- Root package: `edgareldy.springboottutorial`
- DTOs: Java `record` rather than classes (immutability, less boilerplate)
- No business logic in controllers: delegate to the service layer only
- **Contract/implementation services**: the interface (`XxxService`) lives at the root of `service/`, its implementation (`XxxServiceImpl`) lives in `service/impl/`. Controllers and tests only depend on the interface (injected by interface type), never directly on the implementation.
- Any service method that writes to the database is annotated `@Transactional` (on the implementation)
- Every controller returns an `ApiResponse<T>` (see [Standard response format](#standard-response-format)), including `GlobalExceptionHandler` for errors
- List responses are always paginated (`Page<T>` → `PageResponse<T>` DTO)
- Endpoint names are plural, `kebab-case` when composed
- Every endpoint documented with `@Operation(summary = "...")`
- All REST endpoints are versioned under the `/api/v1` prefix (e.g. `/api/v1/categories`), so the API can introduce a breaking `/api/v2` later without touching existing clients

## Concepts covered

- Layered architecture (controller / service / repository)
- Spring Data JPA: relations, derived queries, `@Query`, pagination, sorting, projections
- DTOs and mapping (MapStruct)
- Validation (Bean Validation)
- Centralized exception handling
- Transactions (`@Transactional`)
- Schema migrations (Flyway)
- API documentation (OpenAPI / Swagger UI)
- Observability (Actuator, structured logging)
- Security (Spring Security, JWT, role-based authorization)
- Testing (unit, integration, Testcontainers)
- Multi-environment configuration (Spring profiles)
- `@ConfigurationProperties`
- Application cache (`@Cacheable`)
- Data seeding (`CommandLineRunner`)
- Application events (`ApplicationEventPublisher`)
- Aspect-oriented programming (Spring AOP: `@Around`, `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`)
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions)

## How to follow this tutorial

1. Clone the repository and check out `develop`
2. Create/checkout the `feature/core-architecture` branch and follow its task checklist
3. Continue with `feature/products`, `feature/customers`, `feature/orders`, `feature/auth` in that order
4. Open a Pull Request to `develop` at the end of each branch
5. Run the project with `docker-compose up`, then open Swagger UI at `http://localhost:8080/swagger-ui.html`
