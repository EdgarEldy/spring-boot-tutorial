# Deployment

## How the application is containerized

The project ships with a **multi-stage Dockerfile**. Using multiple stages is a production best practice: the first stage compiles the application using a full JDK and Maven image; the second stage copies only the resulting JAR into a minimal JRE image. This keeps the final image small and free of build tools.

```dockerfile
# Stage 1 - build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline   # download deps first (layer cache)
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2 - runtime
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
RUN useradd --system --create-home appuser \
    && mkdir -p /app/logs \
    && chown -R appuser:appuser /app
USER appuser
COPY --from=build /workspace/target/spring-boot-tutorial-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why copy `pom.xml` before `src/`?** Docker builds images layer by layer. By copying `pom.xml` first and running `dependency:go-offline`, Maven downloads all dependencies into a dedicated layer. This layer is cached as long as `pom.xml` doesn't change - a typical code edit only invalidates the `src/` layer and the compilation step, not the dependency download.

**Why a non-root user (`appuser`)?** Running as `root` inside a container is a security risk: a vulnerability in the app or its dependencies could give an attacker root access to the host. Using a system user limits the blast radius.

---

## docker-compose.yml

Three services are declared:

| Service | Image | Purpose |
|---|---|---|
| `db` | `postgres:16` | PostgreSQL database with a persistent volume |
| `app` | Built from `Dockerfile` | The Spring Boot application (prod profile) |
| `mailhog` | `mailhog/mailhog` | Local SMTP server for email testing |

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB:       ${POSTGRES_DB:-spring_boot_tutorial}
      POSTGRES_USER:     ${POSTGRES_USER:-spring_boot_tutorial}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-spring_boot_tutorial}
    ports:
      - "5432:5432"
    volumes:
      - db-data:/var/lib/postgresql/data   # data survives container restarts
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-spring_boot_tutorial}"]
      interval: 5s
      retries: 5

  app:
    build: .
    depends_on:
      db:
        condition: service_healthy   # wait for PostgreSQL to be ready
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL:      ${DB_URL:-jdbc:postgresql://db:5432/spring_boot_tutorial}
      DB_USERNAME: ${DB_USERNAME:-spring_boot_tutorial}
      DB_PASSWORD: ${DB_PASSWORD:-spring_boot_tutorial}
      JWT_SECRET:  ${JWT_SECRET:-docker-compose-demo-secret-...}
      ...
    ports:
      - "8080:8080"
```

**`depends_on` with `condition: service_healthy`** ensures the app container does not start until PostgreSQL is ready to accept connections. Without this, the app would start, try to connect to PostgreSQL before it finishes initializing, and crash.

**`${VAR:-default}` syntax** reads a variable from the `.env` file (or the shell environment) and falls back to the default if not set. This keeps docker-compose.yml usable out of the box for demos while allowing proper secrets in real deployments.

**Named volume `db-data`** persists database files across `docker compose down` / `up` cycles. Use `docker compose down -v` to also delete the volume (e.g. for a clean reset).

---

## First-time setup

```bash
# 1. Copy the environment template
cp .env.example .env

# 2. Set the minimum required variables in .env
#    - POSTGRES_PASSWORD (database password)
#    - JWT_SECRET (long, random string - at least 32 characters)

# 3. Build and start
docker compose up --build

# 4. On subsequent starts (image already built)
docker compose up
```

Flyway runs all migrations automatically when the application starts. On a fresh database this creates all tables; on an existing database it only applies new migrations.

---

## Useful commands

```bash
# Start all services in the foreground (see logs)
docker compose up

# Start in the background
docker compose up -d

# View logs of the app service
docker compose logs -f app

# Stop without deleting data
docker compose down

# Stop and delete the database volume (full reset)
docker compose down -v

# Rebuild the app image after a code change
docker compose up --build app

# Connect to the database directly
docker compose exec db psql -U spring_boot_tutorial -d spring_boot_tutorial
```

---

## CI/CD (GitHub Actions)

The project uses GitHub Actions for continuous integration. The workflow is defined in `.github/workflows/ci.yml` and runs on every push and pull request on any branch.

```yaml
name: CI
on:
  push:
    branches: ["**"]
  pull_request:
    branches: ["**"]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "17"
          cache: maven        # caches ~/.m2 between runs
      - run: ./mvnw -B clean verify
```

`./mvnw -B clean verify` compiles the project, runs all tests (including Testcontainers), and fails the build if any test fails. GitHub Actions provides Docker out of the box on `ubuntu-latest`, so Testcontainers works without any extra setup.

The Maven cache (`cache: maven`) stores the local Maven repository between runs, so dependencies are only downloaded once per cache key instead of on every push.

---

## Production considerations

### JWT_SECRET

`application-prod.yml` reads `${JWT_SECRET}` **without a default value**. The application will refuse to start if this variable is not set. This is intentional: a default JWT secret would be the same across every deployment, making it possible to forge tokens for one deployment and use them against another.

Generate a strong secret with:
```bash
openssl rand -base64 48
```

### Swagger UI in production

Swagger UI is currently accessible in all profiles at `/swagger-ui.html`. Before going to production, consider one of:
- Disabling it with `springdoc.api-docs.enabled=false` in `application-prod.yml`
- Restricting access to it via `SecurityConfig` (require ADMIN role)
- Moving it behind a VPN or internal network

### HTTPS

The application does not handle TLS termination. In a production deployment, place it behind a reverse proxy (nginx, Traefik, AWS ALB, etc.) that terminates HTTPS and forwards HTTP to port 8080.

### Email

The `dev` profile points to MailHog (a local SMTP trap that never actually sends emails). In production, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD` must point to a real SMTP server (e.g. Amazon SES, SendGrid, Mailgun). MailHog's web UI at `http://localhost:8025` lets you inspect outgoing emails during local development.
