# spring-gift-test

선물하기 서비스의 인수 테스트 체계를 고도화한 프로젝트입니다.

## 빠른 시작

```bash
./gradlew cucumberTest
```

이 한 줄로 Docker 이미지 빌드 → 컨테이너 시작 → 테스트 실행 → 정리까지 자동 수행됩니다.

---

## 요구사항 1: Cucumber BDD 적용

RestAssured 기반 인수 테스트를 Cucumber BDD 형식으로 전환하여, 비개발자도 이해할 수 있는 테스트 시나리오를 작성합니다.

### 구조

```
src/test/resources/features/       # 한글 Gherkin 시나리오
├── category.feature               # 카테고리 관리 (2개 시나리오)
├── product.feature                # 상품 관리 (2개 시나리오)
└── gift.feature                   # 선물하기 (4개 시나리오)

src/test/java/gift/acceptance/
├── CucumberTest.java              # JUnit Platform Suite 러너
├── CucumberSpringConfiguration.java  # Spring Boot 통합 설정
├── TestContext.java                # 시나리오별 공유 상태 (@ScenarioScope)
├── DatabaseCleanup.java           # 시나리오별 DB 초기화
└── steps/
    ├── CategorySteps.java         # 카테고리 Step Definitions
    ├── ProductSteps.java          # 상품 Step Definitions
    └── GiftSteps.java             # 선물 Step Definitions
```

### 시나리오 예시

```gherkin
# language: ko

기능: 선물하기

  배경:
    먼저 "전자기기" 카테고리를 생성한다
    그리고 카테고리에 이름이 "노트북"이고 가격이 1500000인 상품을 생성한다
    그리고 보내는 회원을 생성한다
    그리고 받는 회원을 생성한다

  시나리오: 재고가 충분하면 선물을 보낼 수 있다
    먼저 재고가 5인 옵션을 생성한다
    만일 3개의 선물을 보낸다
    그러면 응답 상태코드가 200이다
```

### 핵심 설계

- **한글 Gherkin**: `io.cucumber.java.ko` 패키지의 `@먼저`, `@만일`, `@그러면` 어노테이션 사용
- **시나리오 격리**: `@ScenarioScope`로 시나리오마다 새로운 TestContext 생성, `@Before` 훅으로 DB 초기화
- **Step 재사용**: 카테고리 생성 등 공통 Step은 여러 Feature에서 재사용

---

## 요구사항 2: PostgreSQL + Docker Compose 통합

H2 인메모리 DB를 PostgreSQL로 전환하고, Docker Compose로 테스트 환경을 자동화합니다.

### 구성 파일

| 파일                                                   | 역할                      |
|------------------------------------------------------|-------------------------|
| `docker-compose.yml`                                 | PostgreSQL + App 서비스 정의 |
| `src/test/resources/application-cucumber.properties` | 테스트용 PostgreSQL 접속 설정   |

### Spring 프로파일 분리

- **기본 프로파일** (`application.properties`): H2 인메모리 DB (개발용)
- **cucumber 프로파일** (`application-cucumber.properties`): PostgreSQL (테스트용)

`@ActiveProfiles("cucumber")`로 테스트 시 자동 적용됩니다.

### DB 초기화

각 시나리오 실행 전 PostgreSQL의 `TRUNCATE ... RESTART IDENTITY CASCADE`로 모든 테이블을 초기화하여 테스트 격리를 보장합니다.

---

## 요구사항 3: Application 컨테이너화

Spring Boot 애플리케이션까지 Docker 컨테이너로 실행하여, 프로덕션과 동일한 환경에서 E2E 테스트를 수행합니다.

### 아키텍처

```
Host (테스트 JVM)
  │
  ├─ RestAssured ──→ localhost:28080 ──→ [Docker] app:8080
  │                                          │
  └─ JdbcTemplate ─→ localhost:5432 ──→ [Docker] postgres:5432
                                             ▲
                                    app ─────┘ (postgres:5432)
```

- 테스트 코드는 호스트에서 실행
- 앱과 DB는 Docker 컨테이너에서 실행
- 앱 컨테이너는 Docker 네트워크 내에서 `postgres` 서비스명으로 DB에 접근

### Dockerfile (Multi-stage build)

```dockerfile
# Builder: Gradle로 빌드
FROM gradle:8.4-jdk21 AS builder
...
RUN gradle bootJar --no-daemon -x test

# Runtime: 경량 JRE 이미지
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
```

- Builder 스테이지에서 빌드만 수행하고, Runtime 스테이지에는 JAR만 복사하여 이미지 크기를 최소화합니다.

### 실행 방법

```bash
# 전체 자동 실행
./gradlew cucumberTest

# 단계별 수동 실행
./gradlew dockerBuild                        # Docker 이미지 빌드
./gradlew dockerUp                           # 컨테이너 시작
curl http://localhost:28080/api/categories   # 앱 응답 확인
./gradlew cucumberTest                       # 테스트 실행
./gradlew dockerDown                         # 컨테이너 정리
```

### Gradle 태스크

| 태스크                      | 설명                        |
|--------------------------|---------------------------|
| `./gradlew cucumberTest` | 빌드 → 시작 → 테스트 → 정리 전체 자동화 |
| `./gradlew dockerBuild`  | Docker 이미지 빌드             |
| `./gradlew dockerUp`     | 전체 컨테이너 시작 (빌드 포함)        |
| `./gradlew dockerDown`   | 전체 컨테이너 종료 및 볼륨 삭제        |

### 테스트 리포트

테스트 실행 후 `build/reports/cucumber/cucumber-report.html`에서 상세 결과를 확인할 수 있습니다.

### 트러블슈팅

```bash
docker ps                                     # 컨테이너 상태 확인
docker logs spring-gift-test-kakao-app-1      # 앱 로그 확인
docker logs spring-gift-test-kakao-postgres-1 # DB 로그 확인
./gradlew dockerDown                          # 잔여 컨테이너 정리
```
