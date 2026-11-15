# Installation and first run

This guide walks you through cloning, configuring, and starting the project from scratch.
You will have a running API, a Postgres database, and a mail catcher in a few minutes.

---

## Prerequisites

Install each tool below before continuing. Java version matters: Spring Boot 3.5.16 requires
Java 17 and will not start on Java 8 or Java 21 without explicit configuration.

| Tool | Version | How to verify |
|---|---|---|
| Java | 17 (LTS) | `java -version` |
| Maven | via `./mvnw` wrapper (no install needed) | `./mvnw -v` |
| Docker | 24+ | `docker -v` |
| Docker Compose | 2.x (bundled with Docker Desktop) | `docker compose version` |
| Git | any recent | `git --version` |

> **Why Docker?** The application needs PostgreSQL 16 and MailHog (SMTP catcher for
> development). Docker Compose starts all three services (the API, the database, and the mail
> catcher) with a single command, without installing Postgres locally.

---

## Option A - Full stack via Docker Compose (recommended)

This option builds and starts the Spring Boot app, PostgreSQL, and MailHog together.

```bash
git clone https://github.com/EdgarEldy/spring-boot-tutorial.git
cd spring-boot-tutorial
cp .env.example .env          # review and adjust values if needed
docker compose up --build
```

Docker Compose starts three containers:

| Container | Role | Port |
|---|---|---|
| `spring-boot-api` | Spring Boot application | 8080 |
| `spring-boot-db` | PostgreSQL 16 | 5432 |
| `mailhog` | SMTP catcher with web UI | 8025 |

The app is ready when you see a line like `Started SpringBootTutorialApplication in X seconds`
in the Docker logs. Open `http://localhost:8080/swagger-ui.html` to confirm.

---

## Option B - Local Maven run (dev profile)

Use this option when you want hot reload via DevTools or when you are iterating on code.

**Step 1 - Start Postgres and MailHog:**

```bash
docker compose up spring-boot-db mailhog
```

**Step 2 - Run the application:**

```bash
# Linux / macOS
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev

# Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"; ./mvnw spring-boot:run `
  -Dspring-boot.run.profiles=dev
```

> **Why specify JAVA_HOME?** If your system default is Java 21 or Java 8, the Maven wrapper
> will pick that up instead. Setting `JAVA_HOME` explicitly pins the build to Java 17.

---

## Seed data

When the `dev` profile is active, `DataSeeder` runs on startup and inserts an initial admin
user together with sample categories and products (only if the tables are empty).

| Field | Value |
|---|---|
| Email | `admin@example.com` |
| Password | `Admin1@` |
| Role | `ADMIN` |

Use these credentials for your first login via Swagger UI or a REST client.

---

## First request via Swagger UI

Swagger UI at `http://localhost:8080/swagger-ui.html` lets you try every endpoint without an
external client.

1. Expand **POST /auth/login**, click **Try it out**.
2. Enter the seed credentials and execute.
3. Copy the `access_token` from the response body.
4. Click **Authorize** (top right), paste `Bearer <token>` into the value field, confirm.
5. All subsequent requests in this session will carry the Authorization header.

---

## Stopping services

```bash
# Stop containers (data is kept)
docker compose down

# Stop containers and delete all data volumes (fresh start next time)
docker compose down -v
```

---

## Troubleshooting

**Port 8080 already in use.**
Find and stop the process occupying port 8080, or change `server.port` in
`src/main/resources/application.yml`.

**Testcontainers fails with "Could not find a valid Docker environment".**
Testcontainers probes `/var/run/docker.sock` by default. If your Docker context is
`desktop-linux`, re-run Maven with the socket path set explicitly:

```bash
DOCKER_HOST=unix:///home/youruser/.docker/desktop/docker.sock ./mvnw test
```

**Application fails to start: "JWT_SECRET environment variable not set".**
Copy `.env.example` to `.env` and set a value for `JWT_SECRET` (at least 32 characters).
The `application-prod.yml` profile requires it without a default to avoid accidental
deployment with a weak secret.

**Flyway migration error on startup.**
If you have changed migration files that were already applied, Flyway will refuse to start.
Drop and recreate the database, or run `docker compose down -v` to wipe the volume.
