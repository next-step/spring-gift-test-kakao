# Gift Platform

Spring Boot 기반 선물하기 플랫폼의 인수 테스트 프로젝트입니다.

## 요구사항

- Java 21
- Gradle 8.4 (wrapper 포함)
- Docker + Docker Compose (Cucumber 테스트 실행 시 필요)

## 빌드 및 실행

```bash
./gradlew build      # 빌드 + 테스트
./gradlew bootRun    # 애플리케이션 실행
```

## 테스트 실행

### 단위 테스트 + RestAssured 인수 테스트 (H2)

Docker 없이 실행 가능합니다.

```bash
./gradlew test
```

| 테스트 클래스 | 메서드 수 | DB |
|-------------|----------|-----|
| OptionTest | 2 | - (순수 단위 테스트) |
| CategoryApiTest | 2 | H2 |
| ProductApiTest | 2 | H2 |
| GiftApiTest | 2 | H2 |

### Cucumber BDD 인수 테스트 (PostgreSQL + Docker)

Docker Compose로 PostgreSQL을 자동 관리합니다. 한 줄로 DB 시작 → 테스트 → DB 종료가 이루어집니다.

```bash
./gradlew cucumberTest
```

| Feature 파일 | 시나리오 수 | DB |
|-------------|-----------|-----|
| category.feature | 2 | PostgreSQL |
| product.feature | 2 | PostgreSQL |
| gift.feature | 2 | PostgreSQL |

### 특정 테스트만 실행

```bash
./gradlew test --tests "gift.model.OptionTest"              # 클래스 지정
./gradlew test --tests "gift.model.OptionTest.decrease_*"   # 메서드 패턴
```

## 프로젝트 구조

```
src/
├── main/java/gift/          # 프로덕션 코드 (수정 금지)
│   ├── ui/                  # REST Controllers
│   ├── application/         # Services + DTOs
│   ├── model/               # Entities + Repositories
│   └── infrastructure/      # Config + Implementations
└── test/
    ├── java/gift/
    │   ├── model/           # 단위 테스트 (OptionTest)
    │   ├── cucumber/        # Cucumber BDD 테스트
    │   │   ├── steps/       # Step Definitions
    │   │   └── hooks/       # DB 정리 Hook
    │   ├── ApiTest.java     # RestAssured 베이스 클래스
    │   ├── CategoryApiTest.java
    │   ├── ProductApiTest.java
    │   └── GiftApiTest.java
    └── resources/
        └── features/        # Gherkin 시나리오 (.feature)
```

## 문서

| 문서 | 설명 |
|------|------|
| [SYSTEM_OVERVIEW.md](docs/SYSTEM_OVERVIEW.md) | 도메인 모델, 패키지 구조, API 엔드포인트 |
| [TEST_DESIGN.md](docs/TEST_DESIGN.md) | 단위 vs 인수 테스트, 시나리오 정의 |
| [TEST_STRATEGY.md](docs/TEST_STRATEGY.md) | 행위 선정 기준, 검증 전략, 의사결정 근거 |
| [STEP2_PLAN.md](docs/step2/STEP2_PLAN.md) | 2단계 미션 요구사항 |
| [CUCUMBER_BDD.md](docs/step2/CUCUMBER_BDD.md) | Cucumber BDD 구현 가이드 |
| [RETROSPECTIVE.md](docs/RETROSPECTIVE.md) | AI 페어 프로그래밍 회고 |
