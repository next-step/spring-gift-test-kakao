# PostgreSQL + Docker Compose 통합 계획 (요구사항 2)

## Context

현재 프로젝트는 H2 인메모리 DB를 사용하며, Cucumber BDD 테스트(step1)가 완료된 상태.
요구사항 2는 **H2 → PostgreSQL 전환**과 **Docker Compose로 테스트 환경 자동화**를 목표로 한다.
이를 통해 Production Parity를 달성하고, 누구나 동일한 환경에서 테스트를 재현할 수 있게 한다.

---

## 핵심 설계 결정

### 1. 테스트 분리 전략

| 명령어 | 실행 대상 | DB | 용도 |
|--------|-----------|-----|------|
| `./gradlew test` | RestAssured 행동 테스트만 | H2 (인메모리) | 빠른 피드백, Docker 불필요 |
| `./gradlew cucumberTest` | Cucumber BDD 테스트만 | PostgreSQL (Docker) | Production Parity |

**왜 분리하는가?**
- Cucumber 테스트에 `@ActiveProfiles("cucumber")` 적용 시 PostgreSQL 연결을 시도함
- Docker 없이 `./gradlew test`를 실행하면 Cucumber 테스트가 PostgreSQL 연결 실패로 깨짐
- 따라서 기존 H2 기반 빠른 테스트와 PostgreSQL 기반 통합 테스트를 **Gradle 태스크 레벨에서 분리**해야 함

### 2. DB 정리 방식 변경

| 항목 | H2 (현재) | PostgreSQL (변경 후) |
|------|-----------|---------------------|
| FK 비활성화 | `SET REFERENTIAL_INTEGRITY FALSE` | 불필요 (`CASCADE` 옵션으로 해결) |
| 테이블 정리 | 테이블별 개별 `TRUNCATE` | 단일 `TRUNCATE ... CASCADE` |
| 시퀀스 초기화 | 자동 (H2 특성) | `RESTART IDENTITY` 명시 필요 |
| FK 재활성화 | `SET REFERENTIAL_INTEGRITY TRUE` | 불필요 |

변경 후 정리 SQL:
```sql
TRUNCATE TABLE option, product, category, wish, member RESTART IDENTITY CASCADE;
```

### 3. 테이블명 호환성

| 항목 | H2 | PostgreSQL |
|------|-----|------------|
| 기본 식별자 | 대문자 저장 (`OPTION`) | 소문자 저장 (`option`) |
| Hibernate 생성 | `option` → H2가 `OPTION`으로 저장 | `option` → 그대로 `option` |
| SQL 참조 시 | `"OPTION"` (대문자 명시) | `option` (소문자, 인용 불필요) |

`option`은 PostgreSQL에서 예약어가 아니므로 인용 없이 사용 가능.

---

## 구현 단계

### Step 1: Docker Compose 파일 생성

**새 파일:** `docker-compose.yml` (프로젝트 루트)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "25432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test -d gift_test"]
      interval: 5s
      timeout: 3s
      retries: 5
```

**설계 근거:**
- `postgres:16-alpine`: 경량 이미지로 빠른 다운로드/시작
- `healthcheck`: `pg_isready`로 PostgreSQL이 실제 쿼리를 받을 준비가 되었는지 확인
- 포트 `25432:5432`: 호스트에서 localhost:25432로 접근 가능 (로컬 PostgreSQL과 충돌 방지)
- `--wait` 플래그와 함께 사용 시 healthcheck 통과까지 자동 대기

**네트워크 구조 (요구사항 2 단계):**
```
테스트 코드 (Host) → JDBC → localhost:25432 → PostgreSQL (Docker Container)
Spring Boot App (Host, @SpringBootTest) → localhost:25432 → PostgreSQL (Docker Container)
```

---

### Step 2: build.gradle 수정

**파일:** `build.gradle`

**변경 사항 5가지:**

#### 2-1. PostgreSQL 드라이버 의존성 추가
```gradle
dependencies {
    // 기존 의존성 유지...
    runtimeOnly 'org.postgresql:postgresql'   // 추가
}
```
`runtimeOnly`: 컴파일 시에는 불필요, 런타임에만 JDBC 드라이버가 필요

#### 2-2. 기존 `test` 태스크에서 Cucumber 제외
```gradle
tasks.named('test') {
    useJUnitPlatform()
    exclude 'gift/cucumber/**'   // Cucumber 테스트 패키지 제외
}
```

#### 2-3. `cucumberTest` 태스크 생성
```gradle
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    include 'gift/cucumber/**'   // Cucumber 테스트만 포함
}
```

#### 2-4. Docker Compose 시작/종료 태스크
```gradle
tasks.register('dockerComposeUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('dockerComposeDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}
```
- `--wait`: healthcheck 통과까지 블로킹 대기 → 별도 wait 스크립트 불필요
- `-d`: 백그라운드 실행

#### 2-5. 태스크 간 의존성 연결
```gradle
cucumberTest.dependsOn(dockerComposeUp)
cucumberTest.finalizedBy(dockerComposeDown)
```
- `dependsOn`: cucumberTest 실행 전에 dockerComposeUp 먼저 실행
- `finalizedBy`: cucumberTest 성공/실패 관계없이 dockerComposeDown 실행 (리소스 정리 보장)

**전체 실행 흐름:**
```
./gradlew cucumberTest
  ├── 1. dockerComposeUp (PostgreSQL 시작 + healthcheck 대기)
  ├── 2. cucumberTest (Cucumber 시나리오 실행)
  └── 3. dockerComposeDown (PostgreSQL 종료, 항상 실행)
```

---

### Step 3: Spring Profile 설정 파일 생성

**새 파일:** `src/test/resources/application-cucumber.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:25432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.jpa.hibernate.ddl-auto=create-drop
```

**설계 근거:**
- `jdbc:postgresql://localhost:25432/gift_test`: Docker Compose에서 매핑한 포트로 접근
- `create-drop`: 테스트 컨텍스트 시작 시 스키마 생성, 종료 시 삭제 → 깨끗한 상태 보장
- `spring.datasource.driver-class-name`은 URL에서 자동 감지되므로 불필요
- `hibernate.dialect`도 Spring Boot 3.x에서 자동 감지되므로 불필요

**Profile 활성화 방식:**
- `@ActiveProfiles("cucumber")`가 있는 테스트만 이 설정을 로드
- 없는 테스트(RestAssured 행동 테스트)는 기본 설정(H2)을 사용

---

### Step 4: CucumberSpringConfiguration에 프로파일 적용

**파일:** `src/test/java/gift/cucumber/CucumberSpringConfiguration.java`

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")    // 추가
public class CucumberSpringConfiguration {
}
```

**영향 범위:**
- 이 클래스가 Cucumber 테스트의 Spring 컨텍스트를 정의
- 모든 Cucumber Step Definition이 이 컨텍스트를 공유
- `@ActiveProfiles("cucumber")` → `application-cucumber.properties` 자동 로드

---

### Step 5: CommonStepDefinitions DB 정리 로직 변경

**파일:** `src/test/java/gift/cucumber/CommonStepDefinitions.java`

**변경 전 (H2):**
```java
@Before
public void setUp() {
    RestAssured.port = port;
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
    jdbcTemplate.execute("TRUNCATE TABLE \"OPTION\"");
    jdbcTemplate.execute("TRUNCATE TABLE product");
    jdbcTemplate.execute("TRUNCATE TABLE category");
    jdbcTemplate.execute("TRUNCATE TABLE wish");
    jdbcTemplate.execute("TRUNCATE TABLE member");
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
}
```

**변경 후 (PostgreSQL):**
```java
@Before
public void setUp() {
    RestAssured.port = port;
    jdbcTemplate.execute(
        "TRUNCATE TABLE option, product, category, wish, member RESTART IDENTITY CASCADE"
    );
}
```

**PostgreSQL TRUNCATE 옵션 설명:**
- `RESTART IDENTITY`: 시퀀스(auto-increment)를 1부터 다시 시작
- `CASCADE`: FK로 참조하는 테이블도 함께 TRUNCATE (FK 제약 위반 방지)
- 여러 테이블을 쉼표로 나열하면 한 번에 처리 (더 효율적)

---

### Step 6: README.md 업데이트

실행 방법에 cucumberTest 명령 추가:

```markdown
## 테스트 실행 방법

### 단위/행동 테스트 (H2, Docker 불필요)
./gradlew test

### Cucumber BDD 테스트 (PostgreSQL + Docker)
./gradlew cucumberTest
# Docker Compose가 자동으로 PostgreSQL을 시작/종료합니다
```

---

## 수정 대상 파일 목록

| # | 파일 | 작업 | 변경 규모 |
|---|------|------|----------|
| 1 | `docker-compose.yml` | **신규 생성** | ~15줄 |
| 2 | `build.gradle` | 의존성 추가 + 태스크 분리 | ~20줄 추가 |
| 3 | `src/test/resources/application-cucumber.properties` | **신규 생성** | ~4줄 |
| 4 | `src/test/java/gift/cucumber/CucumberSpringConfiguration.java` | `@ActiveProfiles` 추가 | 2줄 변경 |
| 5 | `src/test/java/gift/cucumber/CommonStepDefinitions.java` | TRUNCATE 문 변경 | 7줄 → 1줄 |
| 6 | `README.md` | 실행 방법 업데이트 | ~5줄 추가 |

---

## 주의 사항

### "OPTION" 테이블명 호환성
- H2는 식별자를 대문자로 저장 → `"OPTION"` (인용 필요)
- PostgreSQL은 소문자로 저장 → `option` (인용 불필요)
- Hibernate의 SpringPhysicalNamingStrategy는 소문자 스네이크 케이스로 테이블 생성
- Cucumber 정리 SQL에서는 `option` (소문자, 인용 없이) 사용

### GenerationType.IDENTITY
- H2: `AUTO_INCREMENT` 사용
- PostgreSQL: `GENERATED BY DEFAULT AS IDENTITY` 사용
- 둘 다 `GenerationType.IDENTITY`로 동일하게 동작 → **엔티티 변경 불필요**

### RESTART IDENTITY
- TRUNCATE 시 시퀀스를 초기화하여 테스트 간 ID 예측 가능성 확보
- 없으면 테스트 실행 순서에 따라 ID가 누적되어 테스트 격리 깨질 수 있음

### `--wait` 플래그
- `docker compose up -d --wait`는 healthcheck가 `healthy` 상태가 될 때까지 대기
- 별도의 wait-for-it 스크립트나 sleep 없이 안정적으로 DB 준비 완료 보장

### 기존 RestAssured 테스트 영향 없음
- `BaseBehaviorTest` 하위 클래스들은 `@ActiveProfiles`가 없으므로 기본 설정(H2) 유지
- `test` 태스크에서 `exclude 'gift/cucumber/**'`로 Cucumber 테스트 제외
- 기존 cleanup.sql 등 H2 전용 SQL 파일은 그대로 유지

---

## 검증 방법

```bash
# 1. 기존 H2 테스트가 여전히 동작하는지 확인 (Docker 불필요)
./gradlew test

# 2. PostgreSQL + Docker Compose로 Cucumber 테스트 실행
./gradlew cucumberTest
# → Docker Compose 자동 시작 → 테스트 실행 → Docker Compose 자동 종료

# 3. Docker가 정리되었는지 확인
docker ps   # postgres 컨테이너가 없어야 함
```

---

## 전체 아키텍처 (요구사항 2 완료 후)

```
┌─────────────────────────────────────────────────┐
│                  Host Machine                    │
│                                                  │
│  ┌──────────────────┐   ┌─────────────────────┐ │
│  │  ./gradlew test   │   │ ./gradlew            │ │
│  │  (RestAssured)    │   │ cucumberTest         │ │
│  │                   │   │ (Cucumber BDD)       │ │
│  │  Spring Boot      │   │  Spring Boot         │ │
│  │  + H2 (in-memory) │   │  + RANDOM_PORT       │ │
│  │                   │   │  │                   │ │
│  │  Docker 불필요     │   │  │ JDBC              │ │
│  └──────────────────┘   │  ▼                   │ │
│                          │  localhost:25432      │ │
│                          └──────────┬──────────┘ │
│                                     │             │
│  ┌──────────────────────────────────▼───────────┐│
│  │          Docker Container                     ││
│  │          PostgreSQL 16                        ││
│  │          - DB: gift_test                      ││
│  │          - Port: 5432                         ││
│  └───────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```
