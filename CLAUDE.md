# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

- Build: `./gradlew build`
- Test all: `./gradlew test`
- Single test class: `./gradlew test --tests gift.SomeTest`
- Run app: `./gradlew bootRun`

## Architecture

Spring Boot 3.5.8 / Java 21 / H2 in-memory DB / Gradle

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
