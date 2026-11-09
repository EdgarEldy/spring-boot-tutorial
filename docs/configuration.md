# Configuration

## How Spring Boot profiles work

Spring Boot lets you define **multiple configuration files** for different environments. The active profile is selected at startup and determines which file is loaded on top of the base `application.yml`.

```
application.yml              <-- always loaded (shared defaults)
application-dev.yml          <-- loaded when profile = dev
application-test.yml         <-- loaded when profile = test
application-prod.yml         <-- loaded when profile = prod
```

Values in a profile file override those in `application.yml`. This means you can define a default in `application.yml` and only override what differs per environment. The active profile is set via:

- `SPRING_PROFILES_ACTIVE=prod` environment variable (used in `docker-compose.yml`)
- `-Dspring-boot.run.profiles=dev` Maven flag (used for local development)
- `spring.profiles.active=test` in `application-test.yml` (picked up by test slices)

---

## Profile comparison

| Setting | `dev` | `test` | `prod` |
|---|---|---|---|
| Datasource | `localhost:5432/spring_boot_db` (defaults) | Testcontainers (auto-wired by `@ServiceConnection`) | Required via env vars |
| `show-sql` | `true` | - | `false` |
| Flyway | enabled | enabled | enabled |
| Mail host | `localhost:1025` (MailHog) | `localhost:1025` (stubbed) | Required via env vars |
| Actuator endpoints exposed | health, info, metrics, appinfo | health | health |
| Log level (project) | DEBUG | - | INFO (root) |
| Log format | plain text | plain text | ECS JSON (structured) |
| Log file | - | - | `logs/app.log` |

---

## Environment variables

### Required in production

These have **no default value** in `application-prod.yml`. The application will fail to start if they are not set.

| Variable | Used for |
|---|---|
| `DB_URL` | JDBC connection URL, e.g. `jdbc:postgresql://db:5432/mydb` |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | HMAC-SHA256 signing key for JWT. Must be long and random (32+ characters). |
| `MAIL_HOST` | SMTP server hostname |
| `MAIL_PORT` | SMTP server port |

### Optional in production

| Variable | Default | Used for |
|---|---|---|
| `MAIL_USERNAME` | (empty) | SMTP auth username |
| `MAIL_PASSWORD` | (empty) | SMTP auth password |
| `MAIL_SMTP_AUTH` | `true` | Enable SMTP authentication |
| `MAIL_SMTP_STARTTLS` | `true` | Enable STARTTLS |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL for links in emails (activation, reset) |
| `APP_FRONTEND_URL` | `http://localhost:4200` | Frontend URL (used for CORS in `dev` profile) |
| `MAIL_FROM` | `noreply@spring-boot-tutorial.com` | Sender address in outgoing emails |
| `POSTGRES_DB` | `spring_boot_tutorial` | Docker Compose: PostgreSQL database name |
| `POSTGRES_USER` | `spring_boot_tutorial` | Docker Compose: PostgreSQL user |
| `POSTGRES_PASSWORD` | `spring_boot_tutorial` | Docker Compose: PostgreSQL password |

> Copy `.env.example` to `.env` and fill in at minimum `JWT_SECRET`, `DB_PASSWORD`, and `POSTGRES_PASSWORD` before running `docker compose up`.

---

## Key application properties

### JWT

```yaml
jwt:
  secret: ${JWT_SECRET:dev-only-jwt-secret-...}
  expiration-ms: 3600000        # access token TTL = 1 hour
  refresh-expiration-ms: 604800000  # refresh token TTL = 7 days
```

These are bound to `JwtProperties` via `@ConfigurationProperties(prefix = "jwt")`. This is a Spring Boot best practice: instead of scattering `@Value("${jwt.secret}")` across multiple beans, all JWT settings are grouped in one immutable record and injected where needed.

### Application URLs

```yaml
app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  frontend-url: ${APP_FRONTEND_URL:http://localhost:4200}
  mail:
    from: ${MAIL_FROM:noreply@spring-boot-tutorial.com}
```

`app.base-url` is used by `EmailServiceImpl` to construct the activation and password-reset links sent in emails.

### Cache (Caffeine)

```yaml
# Configured programmatically in CacheConfig.java
# Named caches: "categories", "products"
# TTL: 10 minutes
# Max entries: 500 per cache
```

`CacheConfig` builds `CaffeineCacheManager` as a Spring bean. Caffeine is a high-performance, in-memory cache library. `@Cacheable("categories")` on `CategoryServiceImpl.findById` stores the result under the key `#id`; `@CacheEvict("categories")` on `update` and `delete` removes the stale entry. This demonstrates Spring's cache abstraction: the service code is unaware of Caffeine specifically - only the abstraction annotations appear there.

### Actuator

```yaml
# dev profile
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,appinfo

# prod profile
management:
  endpoints:
    web:
      exposure:
        include: health
```

Actuator is Spring Boot's built-in monitoring system. It exposes HTTP endpoints that report on the application's health, metrics, and configuration. In prod, only `health` is exposed to avoid leaking internal details. `appinfo` is a **custom Actuator endpoint** implemented in `AppInfoEndpoint.java` using `@Endpoint(id = "appinfo")` - it demonstrates how to create your own endpoint beyond the built-in ones.

### Async executor

```yaml
# Configured in AsyncConfig.java
# Bean name: "emailTaskExecutor"
# Core pool: 2 threads, Max: 5, Queue capacity: 100
```

`@Async("emailTaskExecutor")` on `EmailServiceImpl` methods means email sending does **not block** the HTTP request thread. The annotation tells Spring to run the method on a background thread from the named pool. Without `@Async`, a slow SMTP server would delay the HTTP response for the registering user.

### Scheduling

```yaml
# Activated by @EnableScheduling in SchedulingConfig.java
```

`TokenCleanupScheduler` runs `@Scheduled(cron = "0 0 3 * * *")` - a cron expression meaning "every day at 03:00". It deletes expired tokens from `activation_tokens`, `password_reset_tokens`, and `blacklisted_tokens`, preventing unbounded table growth.

---

## Hibernate DDL strategy

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # in every profile
```

`validate` means Hibernate reads the existing schema and checks that every entity mapping matches a real column. It never creates or modifies tables. If a migration is missing or an entity has a typo in a column name, the app fails to start immediately with a clear error. **Flyway is the single source of truth** for the schema; Hibernate is just a consistency check.

This is safer than `update` (which silently alters tables and can cause data loss in prod) or `create-drop` (which wipes the database on every restart).

---

## Logging

### Dev / test

Plain text logs to the console. The project package is logged at DEBUG level in dev:

```yaml
logging:
  level:
    com.edgareldy.springboottutorial: DEBUG
```

### Production

Structured ECS JSON logs to both console and a file (`logs/app.log`):

```yaml
logging:
  level:
    root: INFO
  file:
    name: logs/app.log
  structured:
    format:
      console: ecs
      file: ecs
```

ECS (Elastic Common Schema) is a standard JSON log format, suitable for ingestion into the Elastic Stack (Elasticsearch + Kibana) or any other structured log platform. Spring Boot 3.4+ supports it natively via the `logging.structured.format` property - no extra library needed.

The `Dockerfile` creates the `/app/logs` directory and assigns it to the `appuser` system user so the application can write to it without root privileges.
