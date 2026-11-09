# Getting Started

## Prerequisites

| Tool | Minimum version | Notes |
|---|---|---|
| Java | 17 (LTS) | The project targets Java 17 explicitly. Set `JAVA_HOME` if multiple JDKs are installed. |
| Maven | 3.9+ | The Maven wrapper (`./mvnw`) is included; no separate installation needed. |
| Docker | 24+ | Required for PostgreSQL and MailHog. Docker Desktop or Docker Engine both work. |
| Git | any recent | - |

---

## Option A - Full stack via Docker Compose

Builds the application image and starts PostgreSQL, MailHog, and the app together.

```bash
# 1. Clone the repository
git clone https://github.com/EdgarEldy/spring-boot-tutorial.git
cd spring-boot-tutorial

# 2. Copy the environment template and fill in the required values
cp .env.example .env
# Edit .env: set at least JWT_SECRET, DB_PASSWORD, POSTGRES_PASSWORD

# 3. Build and start all services
docker compose up --build
```

The app starts with the `prod` profile. Flyway runs all migrations automatically on startup.

| Service | URL |
|---|---|
| REST API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator health | http://localhost:8080/actuator/health |
| MailHog UI | http://localhost:8025 |

---

## Option B - Local Maven run (dev profile)

Runs the app via Maven with hot-reload (DevTools). The database and MailHog run in separate containers.

### 1. Start PostgreSQL

```bash
docker run -d \
  --name sbt-db \
  -e POSTGRES_DB=spring_boot_db \
  -e POSTGRES_USER=spring_boot_db \
  -e POSTGRES_PASSWORD=123456 \
  -p 5432:5432 \
  postgres:16
```

These credentials match the defaults in `application-dev.yml` - no extra configuration needed.

### 2. Start MailHog

```bash
docker compose up -d mailhog
```

### 3. Run the application

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway applies all pending migrations on startup. The app is ready when you see:
```
Started SpringBootTutorialApplication in X.XXX seconds
```

| Service | URL |
|---|---|
| REST API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator (health, info, metrics, appinfo) | http://localhost:8080/actuator |
| MailHog UI | http://localhost:8025 |

---

## Seed data (dev profile only)

`DataSeeder` runs at startup in the `dev` profile and creates a default admin account when no user exists yet:

| Field | Value |
|---|---|
| Username | `admin` |
| Email | `admin@spring-boot-tutorial.com` |
| Password | `Admin1@sbt` |
| Role | ADMIN |

Use this account to access ADMIN-only endpoints directly in Swagger UI without going through the registration flow.

---

## First request

1. Open Swagger UI at http://localhost:8080/swagger-ui.html
2. Expand `POST /api/v1/auth/login` and log in with the seed credentials
3. Copy the `access_token` from the response
4. Click the **Authorize** button at the top of the page, enter `Bearer <token>`, and confirm
5. All authenticated and ADMIN-only endpoints are now available

---

## Stopping the services

**Option A:**
```bash
docker compose down          # stop containers, keep volumes
docker compose down -v       # stop containers AND delete the database volume
```

**Option B:**
```bash
# Stop the app: Ctrl+C in the terminal running ./mvnw spring-boot:run
docker stop sbt-db && docker rm sbt-db
docker compose down mailhog
```
