# PostgreSQL + Docker Compose 학습 기록

## 1. 왜 H2 대신 PostgreSQL을 사용하는가?

### Production Parity (프로덕션 동등성)

프로덕션 환경에서 PostgreSQL을 사용한다면, 테스트도 PostgreSQL에서 실행해야
실제 배포 시 발생할 수 있는 문제를 미리 잡을 수 있다.

H2와 PostgreSQL은 동일한 SQL 표준을 따르지만, 실제로는 차이가 있다:

| 항목 | H2 | PostgreSQL |
|---|---|---|
| 식별자 대소문자 | 대문자로 저장 (`option` → `OPTION`) | 소문자로 저장 (`option` → `option`) |
| TRUNCATE 시 FK 처리 | `SET REFERENTIAL_INTEGRITY FALSE` | `TRUNCATE ... CASCADE` |
| 시퀀스 초기화 | 자동 | `RESTART IDENTITY` 명시 필요 |
| 문자열 함수/타입 | 일부 H2 전용 함수 존재 | 표준 SQL + PostgreSQL 확장 |

이 프로젝트에서도 실제로 H2 → PostgreSQL 전환 시 SQL 호환 문제가 발생했다:
- `"OPTION"` (대문자 인용) → `option` (소문자)으로 변경 필요
- `SET REFERENTIAL_INTEGRITY` → `TRUNCATE ... CASCADE`로 변경 필요

**H2에서는 통과하지만 PostgreSQL에서 실패하는 테스트가 있을 수 있다.**
이것이 Production Parity가 중요한 이유다.

---

## 2. Docker Compose

### 2.1 Docker Compose란?

여러 Docker 컨테이너를 정의하고 한 번에 실행/종료할 수 있는 도구다.
`docker-compose.yml` 파일에 서비스를 선언적으로 정의한다.

### 2.2 docker-compose.yml 구조

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

**각 항목 설명:**

| 항목 | 역할 |
|---|---|
| `services` | 실행할 컨테이너 목록 정의 |
| `image: postgres:16-alpine` | PostgreSQL 16 경량 이미지 사용 |
| `environment` | 컨테이너 내부 환경변수 설정 (DB명, 사용자, 비밀번호) |
| `ports: "25432:5432"` | 호스트 포트:컨테이너 포트 매핑 (로컬 PostgreSQL과 충돌 방지) |
| `healthcheck` | 컨테이너 준비 상태 확인 방법 정의 |

### 2.3 Healthcheck는 왜 필요한가?

컨테이너가 **시작(started)** 된 것과 **준비(ready)** 된 것은 다르다.

```
컨테이너 시작 (Started)
  → PostgreSQL 프로세스 기동 중...
  → 초기화 스크립트 실행 중...
  → DB 생성 중...
  → 연결 수락 준비 완료 (Healthy) ← 이 시점부터 쿼리 가능
```

Healthcheck 없이 바로 테스트를 실행하면, DB가 아직 준비되지 않아 연결 실패가 발생할 수 있다.

`pg_isready`는 PostgreSQL 전용 명령으로, DB가 실제로 쿼리를 받을 수 있는 상태인지 확인한다.

### 2.4 주요 명령어

```bash
docker compose up -d --wait   # 백그라운드 실행 + healthcheck 통과 대기
docker compose down           # 컨테이너 + 네트워크 정리
docker compose ps             # 실행 중인 컨테이너 상태 확인
docker compose logs postgres  # PostgreSQL 로그 확인
```

- `-d`: 백그라운드(detached) 모드
- `--wait`: 모든 서비스의 healthcheck가 healthy 상태가 될 때까지 블로킹 대기

---

## 3. Spring Profile로 DB 분리

### 3.1 Spring Profile이란?

환경(개발/테스트/운영)에 따라 다른 설정을 적용하는 메커니즘이다.
`application-{profile}.properties` 파일을 만들면, 해당 프로파일 활성화 시 자동으로 로드된다.

### 3.2 프로파일 구조

```
src/main/resources/
  └── application.properties              ← 기본 설정 (H2, 개발용)

src/test/resources/
  └── application-cucumber.properties     ← cucumber 프로파일 (PostgreSQL, 테스트용)
```

**application.properties** (기본 — H2):
```properties
spring.application.name=gift
spring.jpa.open-in-view=false
# DB 설정 없음 → Spring Boot가 H2 인메모리 DB를 자동 구성
```

**application-cucumber.properties** (PostgreSQL):
```properties
spring.datasource.url=jdbc:postgresql://localhost:25432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.jpa.hibernate.ddl-auto=create-drop
```

### 3.3 프로파일 활성화 방법

테스트 코드에서 `@ActiveProfiles("cucumber")`를 선언한다:

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")    // ← application-cucumber.properties 로드
public class CucumberSpringConfiguration {
}
```

**동작 원리:**
1. Spring Boot가 기본 `application.properties`를 먼저 로드
2. `@ActiveProfiles("cucumber")`가 있으므로 `application-cucumber.properties`를 추가 로드
3. 동일한 키가 있으면 프로파일 설정이 기본 설정을 **덮어씀(override)**
4. 결과적으로 H2 대신 PostgreSQL에 연결

### 3.4 테스트 분리 결과

| 명령어 | 프로파일 | DB | 대상 |
|---|---|---|---|
| `./gradlew test` | default | H2 | RestAssured 행동 테스트 |
| `./gradlew cucumberTest` | cucumber | PostgreSQL | Cucumber BDD 테스트 |

`@ActiveProfiles`가 `CucumberSpringConfiguration`에만 있으므로,
RestAssured 테스트(`BaseBehaviorTest` 하위)는 영향을 받지 않고 기본 H2를 사용한다.

---

## 4. Gradle 태스크 분리

### 4.1 왜 태스크를 분리하는가?

Cucumber 테스트가 `@ActiveProfiles("cucumber")`로 PostgreSQL에 연결하므로,
Docker 없이 `./gradlew test`를 실행하면 Cucumber 테스트가 실패한다.

따라서:
- `test` 태스크에서 Cucumber 테스트를 **제외**
- `cucumberTest` 태스크를 새로 만들어 Cucumber 테스트만 **포함**

### 4.2 태스크 구성

```gradle
// 기존 test에서 Cucumber 제외
tasks.named('test') {
    useJUnitPlatform()
    exclude 'gift/cucumber/**'
}

// Cucumber 전용 태스크
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    include 'gift/cucumber/**'
}

// Docker Compose 시작/종료
tasks.register('dockerComposeUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('dockerComposeDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}

// 의존성 연결
cucumberTest.dependsOn(dockerComposeUp)
cucumberTest.finalizedBy(dockerComposeDown)
```

### 4.3 태스크 의존성 키워드

| 키워드 | 의미 | 예시 |
|---|---|---|
| `dependsOn` | 이 태스크 실행 **전에** 먼저 실행 | `cucumberTest` 전에 `dockerComposeUp` |
| `finalizedBy` | 이 태스크 **성공/실패 관계없이** 이후 실행 | `cucumberTest` 후 항상 `dockerComposeDown` |

`finalizedBy`가 중요한 이유: 테스트가 실패해도 Docker 컨테이너가 반드시 정리된다.
이것이 없으면 실패 시 PostgreSQL 컨테이너가 계속 떠 있게 된다.

### 4.4 실행 흐름

```
./gradlew cucumberTest
  │
  ├── 1. dockerComposeUp          (dependsOn)
  │      docker compose up -d --wait
  │      → PostgreSQL 시작 → healthcheck 대기 → Healthy
  │
  ├── 2. cucumberTest
  │      Cucumber 시나리오 7개 실행
  │      Spring Boot → PostgreSQL (localhost:25432)
  │
  └── 3. dockerComposeDown         (finalizedBy, 항상 실행)
         docker compose down
         → 컨테이너 + 네트워크 정리
```

---

## 5. 테스트 격리 (Test Isolation)

### 5.1 각 시나리오마다 DB 초기화

```java
@Before
public void setUp() {
    RestAssured.port = port;
    jdbcTemplate.execute(
        "TRUNCATE TABLE option, product, category, wish, member RESTART IDENTITY CASCADE"
    );
}
```

매 시나리오 실행 전에 모든 테이블을 비운다.

### 5.2 PostgreSQL TRUNCATE 옵션

| 옵션 | 역할 |
|---|---|
| `RESTART IDENTITY` | 시퀀스(auto-increment)를 1부터 다시 시작 |
| `CASCADE` | FK로 참조하는 테이블도 함께 TRUNCATE |

**RESTART IDENTITY가 필요한 이유:**
없으면 시퀀스가 계속 증가하여 시나리오마다 다른 ID가 생성된다.
테스트에서 `INSERT INTO member (id, ...) VALUES (1, ...)`처럼 명시적 ID를 사용하는 경우,
시퀀스가 초기화되지 않으면 충돌이 발생할 수 있다.

**CASCADE가 필요한 이유:**
`product` 테이블이 `category`를 FK로 참조하므로, `category`만 TRUNCATE하면 FK 위반이 발생한다.
`CASCADE`를 사용하면 참조하는 테이블도 함께 정리된다.

### 5.3 H2와의 차이

| 작업 | H2 | PostgreSQL |
|---|---|---|
| FK 비활성화 | `SET REFERENTIAL_INTEGRITY FALSE` | 불필요 (`CASCADE`로 해결) |
| 테이블 정리 | 테이블별 개별 TRUNCATE | 한 문장으로 일괄 TRUNCATE |
| FK 재활성화 | `SET REFERENTIAL_INTEGRITY TRUE` | 불필요 |
| 시퀀스 초기화 | 자동 | `RESTART IDENTITY` 명시 |

PostgreSQL 방식이 더 간결하고 한 줄로 끝난다.

---

## 6. 네트워크 구조 이해

```
┌───────────────────────────────────────┐
│            Host Machine               │
│                                       │
│  ┌─────────────────────────────────┐  │
│  │  ./gradlew cucumberTest         │  │
│  │                                 │  │
│  │  Spring Boot (RANDOM_PORT)      │  │
│  │       │                         │  │
│  │       │ JDBC                    │  │
│  │       ▼                         │  │
│  │  localhost:25432                 │  │
│  └───────────┬─────────────────────┘  │
│              │ port mapping            │
│  ┌───────────▼─────────────────────┐  │
│  │  Docker Container               │  │
│  │  PostgreSQL 16                  │  │
│  │  - DB: gift_test                │  │
│  │  - 내부 포트: 5432 (호스트: 25432) │  │
│  └─────────────────────────────────┘  │
└───────────────────────────────────────┘
```

- 테스트 코드와 Spring Boot는 **호스트(Host)** 에서 실행된다
- PostgreSQL은 **Docker 컨테이너** 안에서 실행된다
- `ports: "25432:5432"` 매핑으로 호스트의 `localhost:25432`가 컨테이너의 `5432`로 연결된다 (로컬 PostgreSQL 5432 포트와 충돌 방지)

---

## 7. hibernate.ddl-auto 옵션

`application-cucumber.properties`에서 `spring.jpa.hibernate.ddl-auto=create-drop`을 사용한다.

| 옵션 | 동작 | 용도 |
|---|---|---|
| `none` | 아무것도 안 함 | 운영 환경 (Flyway/Liquibase 사용 시) |
| `validate` | 엔티티와 스키마 일치 여부만 확인 | 운영 환경 |
| `update` | 스키마를 엔티티에 맞게 변경 (컬럼 추가 등) | 개발 환경 |
| `create` | 기존 스키마 삭제 후 재생성 | 테스트 환경 |
| `create-drop` | 시작 시 생성, 종료 시 삭제 | 테스트 환경 (가장 깨끗) |

`create-drop`을 사용하면 Spring 컨텍스트가 시작될 때 JPA 엔티티 기반으로 테이블을 자동 생성하고,
종료 시 삭제한다. 별도의 DDL 스크립트가 필요 없다.

---

## 8. 주요 의사결정과 이유

| 결정 | 이유 |
|---|---|
| `postgres:16-alpine` 이미지 | 경량 이미지로 다운로드/시작이 빠름. alpine 기반 약 80MB |
| `--wait` 플래그 사용 | 별도의 wait-for-it 스크립트 없이 healthcheck 대기 가능 |
| `test`/`cucumberTest` 분리 | Docker 없이도 H2 테스트를 빠르게 실행 가능하도록 |
| `finalizedBy`로 정리 보장 | 테스트 실패 시에도 컨테이너가 반드시 종료되도록 |
| `create-drop` DDL 전략 | 마이그레이션 도구 없이 엔티티 기반으로 스키마 자동 생성 |
| `TRUNCATE ... CASCADE` | 한 줄로 모든 테이블 정리 + FK 문제 자동 해결 |

---

## 9. 탐구 질문과 답변

**Q. Docker Compose의 services, volumes는 무엇인가?**

- `services`: 실행할 컨테이너를 정의한다. 각 서비스는 하나의 컨테이너에 대응한다.
  이 프로젝트에서는 `postgres` 하나만 정의했다.
- `volumes`: 컨테이너 내부 데이터를 호스트에 영구 저장하는 방법이다.
  테스트용 DB는 일회성이므로 volume을 사용하지 않는다. `docker compose down`하면 데이터가 사라진다.

**Q. Health check는 왜 필요한가?**

컨테이너가 시작(started)된 것과 서비스가 준비(ready)된 것은 다르다.
PostgreSQL 컨테이너가 시작되어도, 내부에서 초기화(DB 생성, 사용자 설정 등)가 진행 중일 수 있다.
Healthcheck는 `pg_isready` 명령으로 실제 연결 가능 여부를 확인하여,
테스트가 시작되기 전에 DB가 완전히 준비되었음을 보장한다.

**Q. Spring Profile은 어떻게 동작하는가?**

1. Spring Boot는 항상 `application.properties`를 기본으로 로드한다
2. 활성 프로파일이 있으면 `application-{profile}.properties`를 추가로 로드한다
3. 프로파일 설정이 기본 설정과 겹치면 프로파일 설정이 우선한다 (override)

활성화 방법:
- 코드: `@ActiveProfiles("cucumber")`
- 환경변수: `SPRING_PROFILES_ACTIVE=cucumber`
- JVM 옵션: `-Dspring.profiles.active=cucumber`

**Q. Gradle Task에서 Shell 스크립트를 어떻게 실행하는가?**

`Exec` 타입 태스크를 사용한다:
```gradle
tasks.register('dockerComposeUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}
```
`commandLine`은 배열 형태로 명령어와 인자를 받는다.
쉘을 거치지 않고 직접 프로세스를 실행하므로, 파이프(`|`)나 리다이렉트(`>`)는 사용할 수 없다.

**Q. 테스트 실패 시에도 DB를 정리하려면 어떻게 해야 하는가?**

`finalizedBy`를 사용한다:
```gradle
cucumberTest.finalizedBy(dockerComposeDown)
```
`finalizedBy`는 대상 태스크의 성공/실패 **관계없이** 반드시 실행된다.
Java의 `try-finally`와 동일한 개념이다.

**Q. H2 단위 테스트와 PostgreSQL 통합 테스트를 어떻게 분리하는가?**

Gradle의 `include`/`exclude` 패턴으로 테스트 클래스를 분리한다:
```gradle
// test: Cucumber 제외 → H2
tasks.named('test') {
    exclude 'gift/cucumber/**'
}

// cucumberTest: Cucumber만 포함 → PostgreSQL
tasks.register('cucumberTest', Test) {
    include 'gift/cucumber/**'
}
```
패키지 구조(`gift.cucumber`)를 기준으로 분리하므로,
파일 위치만으로 어떤 태스크에서 실행되는지 알 수 있다.

---

## 10. 트러블슈팅

### 실제 발생한 문제: `"OPTION"` 테이블명 호환

**증상:** Gift 관련 시나리오 3개만 `BadSqlGrammarException`으로 실패

**원인:** `GiftStepDefinitions`에서 `INSERT INTO "OPTION"` (대문자 인용)을 사용.
H2는 대문자를 기본으로 사용하지만, PostgreSQL은 소문자를 사용한다.
Hibernate가 생성한 테이블명은 `option` (소문자)이므로, `"OPTION"`은 존재하지 않는 테이블이다.

**해결:** `"OPTION"` → `option` (소문자, 인용 없이)으로 변경

**교훈:** H2와 PostgreSQL의 식별자 대소문자 처리가 다르다.
SQL을 직접 작성할 때는 DB별 차이를 주의해야 하며,
이것이 바로 Production Parity 테스트가 필요한 이유다.
