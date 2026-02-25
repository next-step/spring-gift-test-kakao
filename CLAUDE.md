# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

- Build: `./gradlew build`
- Run app: `./gradlew bootRun`
- JUnit tests (H2, no Docker): `./gradlew test`
- JUnit single class: `./gradlew test --tests gift.SomeTest`
- Cucumber tests (Docker + PostgreSQL): `./gradlew cucumberTest`
- Force rerun (skip UP-TO-DATE cache): `./gradlew test --rerun` or `./gradlew cucumberTest --rerun`

### Cucumber test flow

`./gradlew cucumberTest` runs the following automatically:

1. `composeUp` — Build Docker image and start PostgreSQL + Spring app containers
2. `waitForPostgres` — pg_isready health check (max 5 retries)
3. `resetDatabase` — Drop and recreate testdb
4. `waitForApp` — /actuator/health check (max 10 retries)
5. `cucumberTest` — Run Cucumber tests against Docker app (localhost:18080)
6. `composeDown` — Stop and remove containers

### Java version note

Gradle 8.4 requires Java 21. If your default Java is 24, prefix commands with:
```
JAVA_HOME=/path/to/temurin-21 ./gradlew test
```

## Architecture

Spring Boot 3.5.8 / Java 21 / Gradle

- **Runtime DB**: PostgreSQL (Docker) for Cucumber tests, H2 in-memory for JUnit tests
- **Profiles**: `prod` (PostgreSQL, ddl-auto: none), `cucumber` (PostgreSQL, ddl-auto: update)

Layered architecture with four packages under `gift`:

- **ui** — REST controllers (`/api/categories`, `/api/products`, `/api/gifts`)
- **application** — Services (`@Service @Transactional`) and request DTOs (plain classes, not records)
- **model** — JPA entities, repositories (`JpaRepository`), and domain interfaces
- **infrastructure** — External integrations (Kakao API config, `FakeGiftDelivery`)

Key design decisions:
- `GiftDelivery` is a domain interface in `model`, implemented by `FakeGiftDelivery` in `infrastructure` (strategy pattern for swappable delivery)
- `Gift` is a value object (not a JPA entity), constructed in `GiftService` and passed to `GiftDelivery`
- Category/Product create endpoints use form parameter binding (no `@RequestBody`); Gift endpoint uses `@RequestBody` + `@RequestHeader("Member-Id")`
- JPA open-in-view is disabled (`spring.jpa.open-in-view=false`)
- Option/Wish have services but no controllers yet

## Test Structure

### JUnit Acceptance Tests (`./gradlew test`)
- `CategoryAcceptanceTest`, `ProductAcceptanceTest`, `GiftAcceptanceTest`
- Use `@SpringBootTest` with H2 in-memory DB, no Docker required
- Data setup via `@Autowired` repositories

### Cucumber Tests (`./gradlew cucumberTest`)
- Feature files: `src/test/resources/features/` (Korean Gherkin with `# language: ko`)
- Step Definitions: `CommonStepDefinitions`, `CategoryStepDefinitions`, `ProductStepDefinitions`, `GiftStepDefinitions`
- No `@SpringBootTest` — tests run against Docker app container
- Data setup via JDBC to Docker PostgreSQL, API calls to `localhost:18080`
- `SharedContext` holds shared state (IDs, responses) between steps

## Docker

- `Dockerfile` — Multi-stage build (build: `eclipse-temurin:21-jdk`, run: `eclipse-temurin:21-jre-alpine`)
- `docker-compose.yml` — PostgreSQL 16 + Spring app, with health checks
- `.dockerignore` — Excludes `.gradle`, `build`, `.idea`, `.env`, `.git`
