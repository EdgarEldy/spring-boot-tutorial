# Branch: feature/products

This branch adds the first two business resources: Category and Product. It establishes the
CRUD pattern that all subsequent feature branches repeat. If you understand how categories
and products work, you understand how every other list resource works.

---

## What you will learn

- How to cache the result of individual lookups with `@Cacheable` and evict on mutation with `@CacheEvict`
- How to seed development data with `CommandLineRunner` and an idempotency guard
- How to detect and reject concurrent updates with optimistic locking (`@Version`)
- How to build a dynamic query filter with `JpaSpecificationExecutor` and `Specification`
- How to record `created_at` / `updated_at` timestamps automatically with JPA auditing annotations

---

## What it delivers

### Category CRUD

`CategoryService` / `CategoryServiceImpl` manages the full CRUD lifecycle. `findById` is
annotated `@Cacheable(value = "categories", key = "#id")` so repeated lookups for the same
id are served from Caffeine. `update` and `delete` are annotated `@CacheEvict` to remove the
stale entry when the category changes.

`CategoryResponse` gains `createdAt` and `updatedAt` fields (Flyway V4 adds the columns,
`@CreatedDate` / `@LastModifiedDate` on the entity field populate them via `JpaAuditingConfig`).

### Product CRUD

`ProductService` / `ProductServiceImpl` follows the same pattern. `findByIdWithCategory` uses
`@EntityGraph(attributePaths = "category")` to eagerly load the category in a single query,
avoiding the N+1 issue on the detail endpoint.

`@Cacheable` and `@CacheEvict` are applied to `findById`, `update`, and `delete`. The `findAll`
paginated endpoint is not cached, because the result set changes with every sort/filter
parameter combination and the cache hit rate would be near zero.

### Optimistic locking with @Version

`Product` has a `version` field annotated `@Version`. Flyway V3 adds the column. When two
concurrent requests update the same product, Hibernate detects the version mismatch and throws
`OptimisticLockingFailureException`. `GlobalExceptionHandler` maps this to 409 Conflict.

> **Why optimistic, not pessimistic?** Pessimistic locking holds a database row lock for the
> entire request duration. For a REST API where individual requests are short and concurrent
> edits are rare, optimistic locking is cheaper: no lock is held, and conflicts are detected
> only when they actually occur.

### ProductSpecifications

`ProductSpecifications` provides static factory methods that return `Specification<Product>`
predicates: `hasNameLike(String)`, `hasPriceGreaterThanOrEqual(BigDecimal)`,
`hasPriceLessThanOrEqual(BigDecimal)`.

`ProductRepository` extends `JpaSpecificationExecutor<Product>`. `ProductServiceImpl.findAll`
combines the predicates with `Specification.where(...).and(...)` when any filter parameter is
present, falling back to the standard paginated query when none are provided.

> **N+1 on the Specification path:** The `Specification` query does not use `@EntityGraph`,
> so `Product.category` is loaded lazily. If the response includes `categoryName`, each
> product in the page triggers a separate select. This is documented and accepted for the
> filter path, which is used for targeted search, not full-page browsing.

### DataSeeder

`DataSeeder` implements `CommandLineRunner` and is annotated `@Profile("dev")`. It runs once
on startup. An idempotency guard (`categoryRepository.count() == 0`) ensures it only seeds
when the database is empty, so it is safe to restart the application without duplicating data.

It seeds two categories (Electronics, Books) and three products.

### Cache test isolation

`CategoryServiceCacheTest` and `ProductServiceCacheTest` use `@SpringBootTest` with
Testcontainers. Spring reuses the same application context across test methods in the same
class, which means the same `CacheManager` instance is shared. Without cleanup, an entry
written by one test persists and affects the next test's assertions. Both test classes clear
their respective caches in a `@BeforeEach` method.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/Category.java` | Category JPA entity with JPA auditing |
| `src/main/java/.../entity/Product.java` | Product JPA entity with @Version |
| `src/main/java/.../service/CategoryService.java` | Category service interface |
| `src/main/java/.../service/impl/CategoryServiceImpl.java` | Implementation with @Cacheable |
| `src/main/java/.../service/ProductService.java` | Product service interface |
| `src/main/java/.../service/impl/ProductServiceImpl.java` | Implementation with @Cacheable |
| `src/main/java/.../repository/ProductSpecifications.java` | Dynamic filter predicates |
| `src/main/java/.../config/DataSeeder.java` | Dev-only seed data |
| `src/main/resources/db/migration/V3__add_version_to_products.sql` | @Version column |
| `src/main/resources/db/migration/V4__add_audit_columns_to_categories.sql` | Audit timestamps |
| `src/test/java/.../service/CategoryServiceCacheTest.java` | Cache integration test |
| `src/test/java/.../service/ProductServiceCacheTest.java` | Cache integration test |
