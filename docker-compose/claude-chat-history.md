# Docker Compose + Gradle 테스트 환경 구성 대화 기록

## 목표

- Docker Compose로 PostgreSQL 실행 환경 구성
- Spring 프로파일로 테스트/개발 DB 분리
- 테스트 실행 시 DB 자동 시작 및 체크
- `./gradlew cucumberTest` 한 줄로 PostgreSQL 준비 + 테스트 실행 + 정리

---

## 1. Docker Compose 파일 구성

### 초기 작성

`docker-compose.cucumber.yaml`에 PostgreSQL 서비스를 구성했다.

### Health check 추가

PostgreSQL 이미지에 내장된 `pg_isready` 유틸리티를 사용한 health check를 추가했다.

```yaml
healthcheck:
  test: [ "CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}" ]
  interval: 5s
  timeout: 5s
  retries: 5
```

**의사결정 기록:**

- `pg_isready`는 PostgreSQL이 자체 제공하는 CLI 유틸리티로, 서버가 연결을 받을 준비가 되었는지 확인한다.
- `$$`는 Docker Compose가 호스트에서 변수를 치환하지 않고, 컨테이너 내부의 환경변수를 참조하도록 이스케이프한 것이다.

### Health check 확인 방법

```bash
docker ps                                                    # STATUS 열에서 healthy/unhealthy 확인
docker inspect --format='{{.State.Health.Status}}' postgresql # 상태만 조회
docker inspect --format='{{json .State.Health}}' postgresql | python3 -m json.tool  # 상세 로그
```

---

## 2. 환경 변수 파일 분리 (`local-test.env`)

### 동기

- 환경 변수를 compose 파일에 직접 적는 대신 별도 파일로 분리
- 이후 Gradle 커스텀 task, Spring 프로파일에서 재사용 가능

### 현재 내용 (`local-test.env`)

```env
POSTGRES_DB=db-local
POSTGRES_USER=root
POSTGRES_PASSWORD=root
TZ=Asia/Seoul
```

### 의사결정 기록: env 파일 역할 분리

초기에는 `local-test.env`에 Docker용 변수(`POSTGRES_*`)와 Spring용 변수(`DATABASE_*`)를 함께 넣고, Spring용 변수에서 Docker 변수를 `${POSTGRES_DB}` 형태로 참조했다. 하지만 `.env` 파일에서 변수 참조는 표준이 아니라 IDE가 오류로 인식하는 문제가 있었다.

**해결**: env 파일은 Docker 전용(`POSTGRES_*`)으로만 유지하고, Spring 설정(`application-cucumber.yaml`)에서 `${POSTGRES_DB}` 등을 직접 참조하도록 변경했다. `spring.config.import`로 env 파일을 `.properties`로 로드하면, Spring이 자체 플레이스홀더 해결을 통해 `${POSTGRES_DB}` → `db-local`로 치환한다.

### 의사결정 기록: `env_file:` vs `--env-file`

|        | `--env-file` (CLI 플래그)     | `env_file:` (compose 파일 내) |
|--------|----------------------------|----------------------------|
| **대상** | Docker Compose (호스트)       | 컨테이너 내부                    |
| **용도** | compose 파일의 `${VAR}` 변수 치환 | 컨테이너 프로세스에 환경변수 주입         |

- **둘 다 필요하다.** `--env-file`만 쓰면 컨테이너 내부에 환경변수가 주입되지 않아 PostgreSQL이 기본값(`postgres`/`postgres`)으로 기동된다.
- 확인 방법: `docker exec postgresql env | grep POSTGRES`

### 의사결정 기록: `.env` 파일의 변수 참조

`.env` 파일에서 `${POSTGRES_DB}` 같은 변수 참조 지원 여부는 도구마다 다르다:

| 도구                                        | 변수 참조 지원          |
|-------------------------------------------|-------------------|
| Docker Compose `env_file:` / `--env-file` | 미지원 (문자열 그대로)     |
| Shell (`source .env`)                     | 지원                |
| Spring Boot `.properties` 파싱              | 지원 (자체 플레이스홀더 해결) |

---

## 3. Spring 프로파일 분리 (`cucumber`)

### 프로파일 구조

| 명령                       | 프로파일       | DB         | 대상     |
|--------------------------|------------|------------|--------|
| `./gradlew test`         | 없음         | H2 (자동)    | 단위 테스트 |
| `./gradlew cucumberTest` | `cucumber` | PostgreSQL | 인수 테스트 |

### `application-cucumber.yaml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
        highlight_sql: false
        use_sql_comments: true
        globally_quoted_identifiers: true
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}

logging:
  level:
    sql: debug
```

- `ddl-auto: create-drop`: 테스트 시작 시 스키마 생성, 종료 시 삭제
- `${POSTGRES_*}` 플레이스홀더는 `spring.config.import`로 로드된 `local-test.env`에서 해결

### 의사결정 기록: 프로파일명 변경 (`local-test` → `cucumber`)

처음에는 `local-test` 프로파일을 사용했으나, 인수 테스트와의 연관성을 명확히 하기 위해 `cucumber`로 변경했다. 이에 따라:

- `application-local-test.yaml` → `application-cucumber.yaml`
- `docker-compose.local-test.yaml` → `docker-compose.cucumber.yaml`
- Gradle task: `spring.profiles.active` 값을 `cucumber`로 변경

---

## 4. Gradle 커스텀 task 구성 (`build.gradle`)

### 의사결정: env 파일을 Spring에 전달하는 방식

두 가지 접근법을 논의했다:

1. **Gradle에서 env 파일을 파싱**하여 개별 환경변수로 전달 → 파싱 로직 필요, 변수 참조 해결 필요
2. **`spring.config.import`로 Spring에 위임** → Spring이 `.properties` 형식으로 파싱하고 `${}` 참조도 자체 해결

**2번을 채택했다.** Gradle task에서 `systemProperty`로 전달하여 `application-cucumber.yaml`은 env 파일을 몰라도 된다.

```groovy
systemProperty 'spring.config.import', "optional:file:./${cucumberEnvFile}[.properties]"
```

### 의사결정: 테스트 분리 (`test` vs `cucumberTest`)

`./gradlew test` 실행 시 Cucumber `@Suite` 클래스가 JUnit Platform에 발견되어 DB 없이 실패하는 문제가 있었다. Gradle의 `exclude`/`include` 필터로 해결했다:

- `test` task: `exclude '**/cucumber/**'` → cucumber 패키지 제외
- `cucumberTest` task: `include '**/cucumber/**'` → cucumber 패키지만 실행

### 의사결정: `@Profile("cucumber")`의 올바른 사용

`@Profile`은 **Spring 빈 등록을 제어**하는 어노테이션이다. `@Suite` 테스트 러너는 Spring 빈이 아니므로 `@Profile`이 효과가 없다. 테스트 분리는 Gradle 필터가 담당하고, `@Profile("cucumber")`는 실제 Spring 빈에만 적용했다:

| 클래스                             | `@Profile("cucumber")` | 이유                                                              |
|---------------------------------|------------------------|-----------------------------------------------------------------|
| `AcceptanceTestDataManipulator` | ✅ 적용                   | `@Component` — 단위 테스트에서 불필요한 빈 생성 방지                            |
| `AcceptanceContext`             | ✅ 적용                   | `@Component` + `@ScenarioScope` — Cucumber 없이 ScenarioScope 미등록 |
| `CucumberSpringConfig`          | ❌ 불필요                  | Cucumber 엔진이 로드 (Spring component scan 대상 아님)                   |
| `*FeatureTest`                  | ❌ 불필요                  | `@Suite` — Spring 빈이 아님, Gradle 필터로 분리                          |

### 의사결정: Gradle task를 cucumber 전용으로 구성

초기에는 `composeUp`/`composeDown`이 `-Pfile`로 다른 compose 파일을 받을 수 있는 범용 task였다. 실제로는 `cucumberTest`에서만 사용하므로 cucumber 전용으로 정리했다:

- 이름: `cucumberComposeUp`, `cucumberComposeDown`
- `-Pfile` 유연성 제거
- `--env-file`을 `composeDown`에도 추가 (일관성)
- `group`과 `description` 추가 (`./gradlew tasks`에서 카테고리별 표시)

### 최종 Gradle task 구성

```groovy
tasks.named('test') {
    useJUnitPlatform()
    exclude '**/cucumber/**'
}

// Docker Compose + Cucumber
def cucumberComposeFile = 'docker-compose.local-compose.yaml'
def cucumberEnvFile = 'cucumber-test.env'

tasks.register('cucumberComposeUp', Exec) {
    group = 'docker'
    description = 'Start PostgreSQL container for local testing'
    commandLine 'docker', 'compose',
            '--env-file', cucumberEnvFile,
            '-f', cucumberComposeFile,
            'up', '-d', '--wait'
}

tasks.register('cucumberComposeDown', Exec) {
    group = 'docker'
    description = 'Stop PostgreSQL container for local testing'
    commandLine 'docker', 'compose',
            '--env-file', cucumberEnvFile,
            '-f', cucumberComposeFile,
            'down'
}

tasks.register('cucumberTest', Test) {
    group = 'verification'
    description = 'Run Cucumber acceptance tests with Docker PostgreSQL'
    useJUnitPlatform()
    include '**/cucumber/**'
    systemProperty 'spring.profiles.active', 'cucumber'
    systemProperty 'spring.config.import', "optional:file:./${cucumberEnvFile}[.properties]"

    dependsOn cucumberComposeUp
    finalizedBy cucumberComposeDown
}
```

### 실행 흐름

```
./gradlew cucumberTest
  1. cucumberComposeUp → docker compose up -d --wait (healthy 될 때까지 대기)
  2. Test 실행          → cucumber 프로파일 + local-test.env 로드 → PostgreSQL
  3. cucumberComposeDown → docker compose down (항상 실행, finalizedBy)

./gradlew test
  1. cucumber 패키지 제외 → H2로 단위 테스트만 실행
```

---

## 5. 테스트 격리 (Test Isolation)

### 시나리오별 데이터 정리

`CucumberHooks`에서 Cucumber 생명주기 훅으로 관리한다:

- `@Before(order = 0)`: RestAssured 포트 및 로깅 설정
- `@After(order = 0)`: `TestDataManipulator.initAll()`로 모든 데이터 삭제

### 삭제 순서 (FK 제약 준수)

```java
memberRepo.deleteAllInBatch();    // Member (FK 없음)
optionRepo.

deleteAllInBatch();    // Option → Product FK
productRepo.

deleteAllInBatch();   // Product → Category FK
categoryRepo.

deleteAllInBatch();  // Category (FK 없음)
```

### 상태 격리

- `AcceptanceContext`에 `@ScenarioScope` 적용 → 시나리오마다 새 인스턴스 생성
- 시나리오 간 상태 누수 없음

---

## 최종 파일 구조

```
프로젝트 루트
├── docker-compose.cucumber.yaml     ← PostgreSQL 컨테이너 정의
├── local-test.env                   ← Docker + Spring 공용 환경변수
├── build.gradle                     ← test/cucumberTest task 분리
└── src/
    ├── main/resources/
    │   └── application.properties   ← 기본 설정 (H2 auto-config)
    └── test/
        ├── resources/
        │   ├── application.properties       ← 테스트 공통 설정
        │   └── application-cucumber.yaml    ← PostgreSQL datasource
        └── java/gift/
            ├── TestDataManipulator.java     ← 테스트 데이터 조작 인터페이스
            ├── Test*Repository.java         ← 테스트용 JPA 리포지토리
            └── cucumber/
                ├── AcceptanceTestDataManipulator.java  ← @Profile("cucumber")
                ├── AbstractFeature.java
                ├── *FeatureTest.java        ← @Suite 테스트 러너
                ├── config/
                │   ├── CucumberSpringConfig.java  ← Spring 컨텍스트 부트스트랩
                │   └── CucumberHooks.java         ← 시나리오 생명주기 훅
                ├── context/
                │   └── AcceptanceContext.java      ← @Profile("cucumber") + @ScenarioScope
                └── step/
                    ├── CommonStepDefinitions.java
                    ├── CategoryStepDefinitions.java
                    ├── ProductStepDefinitions.java
                    └── GiftStepDefinitions.java
```

---

## 완료된 작업

- [x] Docker Compose로 PostgreSQL 실행 환경 구성
- [x] Spring 프로파일로 테스트/개발 DB 분리 (`cucumber` 프로파일)
- [x] 테스트 실행 시 DB 자동 시작 및 체크 (`--wait` + healthcheck)
- [x] 각 시나리오마다 DB 초기화 (Test Isolation)
- [x] `local-test.env` Docker 전용으로 정리 (DATABASE_* 제거, 따옴표 제거)
- [x] Gradle task를 cucumber 전용으로 정리 (group, description 추가)
- [x] `test`/`cucumberTest` 분리 (exclude/include)
- [x] Cucumber 전용 빈에 `@Profile("cucumber")` 적용
- [x] FeatureTest 러너에서 무의미한 `@Profile` 제거
