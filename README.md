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
- [feature/config](#featureconfig)
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
| `feature/config` | Technical foundation: project structure, configuration, baseline security, Docker, CI. |
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
- Also serves as a teaching base for the other advice types (`@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`) alongside `@Around`
- Can later be reused for auditing (e.g. tracing who created/modified an order) without polluting business code

## feature/config

Technical foundation shared by the whole project, to be merged first into `develop`.

### Tasks

- [ ] Initialize the project via Spring Initializr (Maven, Java 17, Spring Boot 3.5.x)
- [ ] Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-aop`, `flyway-core`, `flyway-database-postgresql`, `postgresql` driver, `lombok`, `mapstruct` + `mapstruct-processor`, `springdoc-openapi-starter-webmvc-ui`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`
- [ ] Test dependencies: `spring-boot-starter-test`, `spring-security-test`, `testcontainers` (junit-jupiter, postgresql)
- [ ] Create the package tree shown above
- [ ] `application.yml`: shared configuration (app name, port, JSON date format)
- [ ] `application-dev.yml`: local datasource, `spring.jpa.show-sql=true`, Flyway enabled
- [ ] `application-test.yml`: Testcontainers datasource
- [ ] `application-prod.yml`: datasource via environment variables, JSON logs
- [ ] Flyway script `V1__init_schema.sql` (categories, products, customers, orders tables with FKs)
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`): handles `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400, field details), `BusinessRuleException` (422), generic `Exception` (500)
- [ ] Standard `ErrorResponse` DTO: `timestamp`, `status`, `error`, `message`, `path`, optional `fieldErrors` list
- [ ] Generic `ApiResponse<T>` (`dto/common/ApiResponse.java`) and `PageResponse<T>` (`dto/common/PageResponse.java`) DTOs, used to wrap every controller response
- [ ] `LoggingAspect` and `ExecutionTimeAspect` (`aspect/`): logging and execution-time measurement via Spring AOP (see [Spring AOP](#spring-aop))
- [ ] `OpenApiConfig`: title, description, version, JWT security scheme in Swagger UI
- [ ] Actuator: expose `health`, `info`, `metrics` in dev; `health` only in prod
- [ ] Structured logging (`logback-spring.xml` or Actuator ECS/JSON config)
- [ ] `CorsConfig`: allow `localhost:3000`/`localhost:5173` in dev
- [ ] Multi-stage `Dockerfile` (Maven build + lightweight JRE image)
- [ ] `docker-compose.yml`: `app` service + `db` service (PostgreSQL 16) with volumes and environment variables
- [ ] `.github/workflows/ci.yml`: Maven build + tests on every push/PR
- [ ] Branch `README` explaining the configuration choices

## feature/products

Includes `Category` and `Product`, given their direct link in the model.

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/categories` | Paginated list of categories |
| GET | `/api/categories/{id}` | Category detail |
| POST | `/api/categories` | Create a category |
| PUT | `/api/categories/{id}` | Update a category |
| DELETE | `/api/categories/{id}` | Delete a category |
| GET | `/api/products` | Paginated list, filterable by `categoryId`, sortable by `productName`/`unitPrice` |
| GET | `/api/products/{id}` | Product detail |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

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
| GET | `/api/customers` | Paginated list, search by name (`?search=`) |
| GET | `/api/customers/{id}` | Customer detail |
| POST | `/api/customers` | Create a customer |
| PUT | `/api/customers/{id}` | Update a customer |
| DELETE | `/api/customers/{id}` | Delete a customer |

### Tasks

- [ ] `Customer` entity
- [ ] `CustomerRepository` with search (`findByEmail`, derived query on first/last name)
- [ ] DTOs `CustomerRequest`/`CustomerResponse` with validation (valid email, phone format)
- [ ] `CustomerMapper`
- [ ] `CustomerService` interface and `CustomerServiceImpl` implementation (in `service/impl`): email uniqueness check on create and update
- [ ] `CustomerController`
- [ ] Unit and integration tests

## feature/orders

### Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/orders` | Paginated list, filterable by `customerId`/`productId` |
| GET | `/api/orders/{id}` | Order detail |
| POST | `/api/orders` | Create an order (`total` computed automatically) |
| PUT | `/api/orders/{id}` | Update an order |
| DELETE | `/api/orders/{id}` | Delete an order |
| GET | `/api/customers/{id}/orders` | Orders for a given customer |

### Tasks

- [ ] `Order` entity with `@ManyToOne` to `Customer` and `Product`
- [ ] `OrderRepository` with join queries (`@Query` with `JOIN`), DTO projection for lists
- [ ] DTOs `OrderRequest` (customerId, productId, quantity) / `OrderResponse` (with summarized customer/product sub-objects)
- [ ] `OrderMapper`
- [ ] `OrderService` interface and `OrderServiceImpl` implementation (in `service/impl`): computes `total = quantity * product.unitPrice`, checks that the customer and product exist
- [ ] `OrderCreatedEvent` application event published after creation, consumed by `OrderCreatedEventListener` (e.g. business logging, future email notification)
- [ ] `OrderController`
- [ ] Unit tests (total computation, business rules) and integration tests

## feature/auth

### Model

- `AppUser` entity (id, username, email, hashed password, roles)
- `Role` enum (`ADMIN`, `USER`)
- Migration `V2__init_users_and_roles.sql`

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Sign up | Public |
| POST | `/api/auth/login` | Sign in, returns a JWT | Public |
| GET | `/api/auth/me` | Current user profile | Authenticated |

### Authorization rules

| Resource | GET | POST/PUT/DELETE |
|---|---|---|
| categories, products | Public | ADMIN |
| customers, orders | Authenticated | ADMIN |

### Tasks

- [ ] `AppUser`, `Role`, `AppUserRepository`
- [ ] `JwtService` (generation, validation, claims extraction)
- [ ] `JwtAuthFilter` (`OncePerRequestFilter`)
- [ ] `UserDetailsServiceImpl`
- [ ] `SecurityConfig` (`SecurityFilterChain`, BCrypt `PasswordEncoder`, per-endpoint authorization rules, CSRF disabled for a stateless API, `STATELESS` session)
- [ ] `AuthService` interface and `AuthServiceImpl` implementation (in `service/impl`), `AuthController` (register, login, me)
- [ ] Custom 401/403 error handling (`AuthenticationEntryPoint`, `AccessDeniedHandler`)
- [ ] Swagger updated to support the "Authorize" button (Bearer token)
- [ ] Integration tests with `spring-security-test` (`@WithMockUser`, allowed/denied access tests)

## Order of work

1. `feature/config` → Pull Request to `develop`
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
2. Create/checkout the `feature/config` branch and follow its task checklist
3. Continue with `feature/products`, `feature/customers`, `feature/orders`, `feature/auth` in that order
4. Open a Pull Request to `develop` at the end of each branch
5. Run the project with `docker-compose up`, then open Swagger UI at `http://localhost:8080/swagger-ui.html`
