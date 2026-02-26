# spring-gift-test

Spring Boot 기반 선물하기 서비스 (카카오 연동)

## 기술 스택

- Java 21
- Spring Boot 3.5.8
- Spring Data JPA
- H2 Database (단위 테스트)
- PostgreSQL (Cucumber 테스트)
- Docker Compose
- Cucumber BDD
- RestAssured

## 사전 요구사항

- JDK 21
- Docker & Docker Compose (Cucumber 테스트용)

## 실행 방법

### 테스트 실행

```bash
# Cucumber BDD 테스트 (PostgreSQL + Docker)
./gradlew cucumberTest

# 기존 JUnit 테스트 (H2)
./gradlew test --tests "gift.*AcceptanceTest"

# 전체 테스트
./gradlew test
```

### Docker 수동 관리

```bash
# App + PostgreSQL 시작
./gradlew dockerUp

# App + PostgreSQL 중지
./gradlew dockerDown

# 또는 docker compose 직접 사용
docker compose up -d --build
docker compose down
```

### 테스트 리포트

테스트 실행 후 리포트 확인:
- JUnit 리포트: `build/reports/tests/test/index.html`
- Cucumber 리포트: `build/reports/cucumber.html`

## 프로젝트 구조

```
├── Dockerfile                  # Spring Boot 앱 컨테이너
├── docker-compose.yml          # App + PostgreSQL 컨테이너
├── src/
│   ├── main/
│   │   ├── java/gift/
│   │   │   ├── ui/             # REST Controller
│   │   │   ├── application/    # Service, Request DTO
│   │   │   ├── model/          # Entity, Repository
│   │   │   └── infrastructure/
│   │   └── resources/
│   │       ├── application.properties           # H2 (기본)
│   │       ├── application-cucumber.properties  # PostgreSQL (로컬)
│   │       └── application-docker.properties    # PostgreSQL (Docker)
│   └── test/
│       ├── java/gift/
│       │   ├── cucumber/
│       │   │   ├── steps/      # Step Definitions
│       │   │   ├── CucumberTest.java
│       │   │   ├── CucumberSpringConfiguration.java
│       │   │   └── TestContext.java
│       │   └── *AcceptanceTest.java  # JUnit 테스트
│       └── resources/features/       # Gherkin Feature 파일
```

## 테스트 환경 분리

| 테스트 유형 | DB | App | 실행 명령 |
|------------|-----|-----|----------|
| JUnit (기존) | H2 | Host JVM | `./gradlew test --tests "gift.*AcceptanceTest"` |
| Cucumber BDD | PostgreSQL (Docker) | Docker | `./gradlew cucumberTest` |

### Production Parity

cucumberTest는 애플리케이션까지 Docker 컨테이너로 실행합니다:

```
┌─────────────────────────────────────────────────────┐
│                   Host Machine                       │
│                                                      │
│  Gradle 테스트 ── HTTP ──▶ localhost:28080 (app)    │
│                ── JDBC ──▶ localhost:5432  (postgres)│
│                                                      │
│  ┌───────────── Docker Network ─────────────┐       │
│  │  gift-app ─── JDBC ──▶ postgres:5432    │       │
│  └───────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────┘
```

이를 통해:
- PostgreSQL과 H2 간 SQL 방언 차이로 인한 문제 사전 발견
- OS, JVM 버전, 환경변수 등 프로덕션과 동일한 환경에서 테스트
