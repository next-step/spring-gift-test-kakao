# spring-gift-test

## 실행 환경

- Java 21
- Spring Boot 3
- Gradle (Groovy DSL)
- Docker (Colima 또는 Docker Desktop)

## 사전 준비

Docker 런타임이 실행 중이어야 합니다.

```bash
# Docker Desktop 사용 시
# Docker Desktop 앱 실행

# Colima 사용 시
colima start
```

## 테스트 구조

| 명령어 | 대상 | DB | 앱 실행 방식 |
|--------|------|----|-------------|
| `./gradlew test` | Java 인수 테스트 (13개) | H2 (인메모리) | 내장 톰캣 (RANDOM_PORT) |
| `./gradlew cucumberTest` | Cucumber 시나리오 (12개) | PostgreSQL (Docker) | Spring Boot (Docker) |

## Java 인수 테스트

Docker 없이 즉시 실행 가능합니다.

```bash
./gradlew test
```

## Cucumber 인수 테스트 (Docker)

### 실행

```bash
./gradlew cucumberTest
```

한 명령으로 빌드 → 컨테이너 시작 → 테스트 → 컨테이너 종료가 자동 실행됩니다.
테스트 실패 시에도 컨테이너는 자동으로 정리됩니다.

### 컨테이너 구조

```
테스트 (Host)
   ↓ HTTP (localhost:28080)
Spring Boot (Docker - gift-test-app)
   ↓ JDBC (postgres:5432)
PostgreSQL (Docker - gift-test-db)
```
