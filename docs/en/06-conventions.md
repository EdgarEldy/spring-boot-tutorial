# Code conventions

The conventions below apply to every file in the project. They keep the codebase consistent
so a reader can understand any feature without learning new patterns. When in doubt, read an
existing feature as the reference implementation.

---

## Java idioms

**DTOs are Java records.**
Records are immutable by design. No setters exist, which prevents accidental mutation
between layers. MapStruct generates mapping code from record component names at compile time.

**Services are injected via their interface.**
Controllers and tests declare a field of type `CategoryService`, never `CategoryServiceImpl`.
This decouples callers from the implementation and makes mocking trivial.

**`@Transactional` on the implementation, not the interface.**
Transactional boundaries belong close to the data access code. Placing `@Transactional`
on the interface would work but would mask the fact that only the concrete proxy is
transactional, not the interface itself. Write operations get `@Transactional`; read
operations get `@Transactional(readOnly = true)` for a minor performance gain.

**snake_case in JSON via `@JsonProperty`.**
Java fields stay in camelCase (consistent with the language convention). The JSON contract
uses snake_case. `@JsonProperty("snake_case")` on individual record components decouples
the two without changing Java naming.

**JPA entities never leave the service layer.**
Controllers receive and return DTOs only. MapStruct mappers convert between entity and DTO
inside the service. This prevents Hibernate's lazy loading from causing
`LazyInitializationException` outside a transaction.

---

## Javadoc

Javadoc is required on every new `class`, `interface`, `enum`, and `record`. Use this format:

```java
/**
 * Brief description of what this type does, in one to three lines.
 * <p>
 * Created edgar.muhamyangabo on 11/15/26
 * Author : edgar.muhamyangabo
 * Date : 11/15/26
 * Project : spring-boot-tutorial
 */
```

Rules:
- Author is always `edgar.muhamyangabo`, regardless of the git user name on the machine.
- Date format is `M/D/YY` with no leading zero (July 4, 2026 is `7/4/26`).
- Write in English.
- Do not use em dashes anywhere in the Javadoc text.

---

## Sensitive data in logs

`LoggingAspect` logs all service method arguments and return values via `toString()`. Records
auto-generate `toString()` from their components. Any record that carries a password, a JWT,
or other sensitive data must override `toString()` to mask those fields:

```java
public record LoginRequest(String email, String password) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
```

Failing to do this will expose credentials in `logs/app.log` in production.

---

## No em dash

Never use the em dash character (`-`) in code, Javadoc, comments, strings, or logs. Use a
hyphen (`-`) instead, or rewrite in two sentences. This rule applies to all project files:
Java, YAML, SQL, Markdown, Dockerfile.

---

## Commits

One file per commit. Each commit message follows Conventional Commits:

```
type(scope): short imperative summary

Body explains why this change was made, written for a developer following
the tutorial. Explain the concept or decision demonstrated here, not just
what changed.
```

Common types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`.
Scope is the package or domain area: `dto`, `controller`, `security`, `flyway`.

A body is required when the change demonstrates a concept worth explaining (the typical
case). It can be omitted for trivial, self-explanatory changes.

Do not add `Co-Authored-By: Claude` or any AI attribution line to commits.

---

## API conventions

- All endpoints are prefixed with `/api/v1`.
- Endpoint paths use plural nouns in kebab-case (`/product-categories`, `/order-items`).
- Every endpoint has an `@Operation(summary = "...")` annotation.
- Every controller response is wrapped in `ApiResponse<T>`.
- Paginated lists use `ApiResponse<PageResponse<T>>` as the response type.
- Validation error messages are written as readable prose, never as the raw field name.
