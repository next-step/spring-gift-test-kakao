# PostgreSQL + Docker Compose 도입 가이드

## 개요

현재 프로젝트는 H2 내장 DB로 모든 테스트를 실행한다. 요구사항 2에 따라 **PostgreSQL + Docker Compose 환경에서 실행되는 새로운 Cucumber BDD 테스트**를 추가한다.

기존 H2 기반 Cucumber 테스트(`cucumberTest`)는 그대로 유지하고, PostgreSQL을 사용하는 별도 태스크(`cucumberPostgresTest`)를 신규 생성한다.

```
./gradlew cucumberTest          ← 기존 유지 (H2)
./gradlew cucumberPostgresTest  ← 신규 추가 (PostgreSQL + Docker)
```

---

## 1부: 개념 정리

### 1.1 Docker Compose 기본 개념

Docker Compose는 여러 컨테이너를 하나의 YAML 파일(`docker-compose.yml`)로 정의하고 관리하는 도구다.

#### services

컨테이너를 정의하는 단위다. 각 서비스는 하나의 컨테이너를 생성한다.

```yaml
services:
  db:
    image: postgres:17
    ports:
      - "5432:5432"
```

- `image`: 사용할 Docker 이미지
- `ports`: 호스트:컨테이너 포트 매핑
- `environment`: 환경 변수 설정

#### volumes

컨테이너의 데이터를 호스트에 영속적으로 저장하는 메커니즘이다. 컨테이너가 삭제되어도 데이터가 유지된다.

```yaml
services:
  db:
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

**테스트 환경에서는 volume을 사용하지 않는 것이 일반적이다.** 테스트는 매번 깨끗한 상태에서 시작해야 하므로, 컨테이너 종료 시 데이터가 사라지는 것이 오히려 바람직하다.

#### networks

서비스 간 통신을 위한 가상 네트워크다. Docker Compose는 기본적으로 하나의 네트워크를 자동 생성하므로, 단일 서비스(PostgreSQL만)를 사용하는 우리 프로젝트에서는 별도 설정이 필요 없다.

### 1.2 Health Check

#### 왜 필요한가

`docker compose up`으로 컨테이너를 시작하면, 컨테이너 자체는 즉시 실행 상태가 된다. 하지만 PostgreSQL이 실제로 연결을 받아들일 준비가 되기까지는 수 초가 걸린다. 이 시간 동안 테스트가 시작되면 **Connection refused** 에러가 발생한다.

Health Check는 컨테이너 내부에서 주기적으로 명령을 실행하여 서비스가 실제로 준비되었는지 확인한다.

#### pg_isready

PostgreSQL이 제공하는 연결 가능 여부 확인 도구다.

```yaml
services:
  db:
    image: postgres:17
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 2s      # 2초 간격으로 확인
      timeout: 5s        # 5초 내 응답 없으면 실패
      retries: 5         # 최대 5번 재시도
      start_period: 10s  # 컨테이너 시작 후 10초간 실패를 무시
```

#### Health Check 상태 확인

```bash
# 컨테이너 상태 확인 (healthy/unhealthy/starting)
docker compose ps

# Health Check 로그 확인
docker inspect --format='{{json .State.Health}}' <container_name>
```

Gradle에서 컨테이너 상태를 확인하는 방법은 2부에서 다룬다.

### 1.3 Spring Profile

#### 동작 원리

Spring Profile은 **환경별로 다른 설정을 적용**하는 메커니즘이다. 활성화된 프로파일에 따라 `application-{profile}.properties` 파일이 로드된다.

**설정 파일 로딩 순서:**
1. `application.properties` (기본 — 항상 로드)
2. `application-{profile}.properties` (활성 프로파일 — 기본 설정을 **덮어씀**)

예를 들어, `cucumber` 프로파일이 활성화되면:
- `application.properties`가 먼저 로드되고
- `application-cucumber.properties`가 그 위에 덮어씌워진다

#### 프로파일 활성화 방법

**방법 1: `@ActiveProfiles` 어노테이션** — 테스트 코드에서 직접 지정

```java
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration { }
```

**방법 2: Gradle 시스템 프로퍼티** — Gradle 태스크에서 외부로 주입

```groovy
task cucumberPostgresTest(type: Test) {
    systemProperty 'spring.profiles.active', 'cucumber'
}
```

방법 2는 **같은 테스트 코드**를 프로파일만 바꿔 다른 환경에서 실행할 수 있다는 장점이 있다. 이 프로젝트에서는 방법 2를 사용한다.

#### 현재 프로젝트에서의 활용: H2 테스트와 PostgreSQL 테스트 분리

| Gradle 태스크 | 프로파일 | 설정 파일 | DB |
|---------------|----------|-----------|-----|
| `test` | (기본) | `application.properties` | H2 (단위 테스트) |
| `cucumberTest` | (기본) | `application.properties` | H2 (기존 BDD 테스트) |
| `cucumberPostgresTest` | cucumber | `application-cucumber.properties` | PostgreSQL (신규 BDD 테스트) |

핵심: **테스트 코드 자체는 동일**하고, Gradle 태스크가 시스템 프로퍼티로 프로파일을 주입하여 DB를 결정한다. 기존 `cucumberTest`는 프로파일 설정이 없으므로 H2를 그대로 사용한다.

### 1.4 Gradle Exec Task

#### Shell 스크립트 실행

Gradle의 `Exec` 타입 태스크로 외부 명령을 실행할 수 있다.

```groovy
task startDB(type: Exec) {
    commandLine 'docker', 'compose', 'up', '-d'
}
```

#### doFirst / doLast

태스크 실행 전후에 추가 로직을 삽입한다.

```groovy
task startDB(type: Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
    doFirst {
        println 'PostgreSQL 컨테이너 시작 중...'
    }
    doLast {
        println 'PostgreSQL 준비 완료'
    }
}
```

#### finalizedBy

**테스트 성공/실패와 관계없이** 후속 태스크를 실행한다. DB 정리에 필수적인 패턴이다.

```groovy
cucumberPostgresTest.finalizedBy stopDB
```

`cucumberPostgresTest`가 실패해도 `stopDB`는 반드시 실행된다. 이는 try-finally와 같은 역할이다.

#### dependsOn

태스크 간 의존 관계를 설정한다.

```groovy
cucumberPostgresTest.dependsOn startDB
```

`cucumberPostgresTest` 실행 전에 `startDB`가 먼저 실행된다.

### 1.5 네트워크 이해: Host vs Container

#### 기본 구조

```
┌─────────────────────────────────────────┐
│              호스트 (macOS / Linux)        │
│                                           │
│  ┌───────────────────┐   ┌────────────┐  │
│  │   Spring Boot      │   │  Docker     │  │
│  │   Test (JVM)       │   │  Container  │  │
│  │                    │   │             │  │
│  │  localhost:5432 ───┼───┼─→ :5432     │  │
│  │                    │   │  PostgreSQL │  │
│  └───────────────────┘   └────────────┘  │
└─────────────────────────────────────────┘
```

#### 핵심 포인트

Spring Boot 테스트는 **호스트에서 JVM으로 실행**된다. Docker 컨테이너 안에서 실행되는 것이 아니다.

따라서 `application-cucumber.properties`에서는 `localhost`로 접근한다:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
```

이것이 가능한 이유는 `docker-compose.yml`에서 포트 매핑을 했기 때문이다:

```yaml
ports:
  - "5432:5432"   # 호스트의 5432 → 컨테이너의 5432
```

#### 컨테이너 간 통신 (참고)

만약 Spring Boot도 컨테이너 안에서 실행된다면, `localhost` 대신 Docker Compose의 서비스 이름(예: `db`)을 사용해야 한다. 하지만 우리 프로젝트에서는 해당되지 않는다.

---

## 2부: 현재 프로젝트에 적용하기 위한 작업

### 현재 상태

| 항목 | 현재 |
|------|------|
| DB | H2 내장 (모든 테스트) |
| Cucumber 설정 | `CucumberSpringConfiguration`에 `@SpringBootTest`만, 프로파일 없음 |
| SQL 스크립트 | `sql/` 디렉토리에 H2 전용 구문 포함 |
| Gradle | `test` (단위), `cucumberTest` (BDD) 태스크 존재 |

### 목표 상태

| 항목 | 목표 |
|------|------|
| 단위 테스트 (`test`) | H2 (변경 없음) |
| 기존 BDD 테스트 (`cucumberTest`) | H2 (변경 없음) |
| **신규 BDD 테스트** (`cucumberPostgresTest`) | PostgreSQL (Docker Compose) |

**원칙: 기존 코드와 테스트는 건드리지 않는다.** 새로운 파일 추가와 Gradle 태스크 신규 정의만으로 목표를 달성한다.

### 작업 1: `docker-compose.yml` 작성

프로젝트 루트에 생성한다.

```yaml
services:
  db:
    image: postgres:17
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test -d gift_test"]
      interval: 2s
      timeout: 5s
      retries: 5
      start_period: 10s
```

**설계 결정:**
- volume 없음 — 테스트 환경이므로 컨테이너 종료 시 데이터 폐기
- `POSTGRES_DB: gift_test` — 컨테이너 시작 시 DB 자동 생성
- Health Check 포함 — `--wait` 플래그와 함께 사용하여 DB 준비 완료를 보장

### 작업 2: `application-cucumber.properties` 생성

`src/test/resources/application-cucumber.properties`에 생성한다.

```properties
# PostgreSQL 연결 설정
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate가 JPA 엔티티 기반으로 테이블 자동 생성
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# PostgreSQL 호환 SQL 스크립트 경로
test.sql.base-path=sql/postgres
```

**설계 결정:**
- `ddl-auto=create-drop` — 테스트 시작 시 테이블 생성, 종료 시 삭제. 테스트 환경에 적합하다
- Dialect 명시 — Hibernate가 PostgreSQL에 맞는 SQL을 생성하도록 보장
- `test.sql.base-path` — Step Definition에서 SQL 스크립트 경로를 결정하는 데 사용 (작업 5 참조)

### 작업 3: PostgreSQL 호환 SQL 스크립트 작성

기존 `sql/` 디렉토리의 H2 전용 SQL은 건드리지 않고, `sql/postgres/` 디렉토리에 PostgreSQL 호환 버전을 **새로 생성**한다.

#### H2 ↔ PostgreSQL 구문 차이

| H2 구문 | 문제 | PostgreSQL 대체 |
|---------|------|----------------|
| `SET REFERENTIAL_INTEGRITY FALSE` | H2 전용 | `TRUNCATE ... CASCADE` |
| `SET REFERENTIAL_INTEGRITY TRUE` | H2 전용 | 불필요 (CASCADE로 해결) |
| `ALTER TABLE member ALTER COLUMN id RESTART WITH 3` | H2 전용 | `ALTER SEQUENCE member_id_seq RESTART WITH 3` |
| `ALTER TABLE category ALTER COLUMN id RESTART WITH 3` | H2 전용 | `ALTER SEQUENCE category_id_seq RESTART WITH 3` |

#### `sql/postgres/common-init.sql` (신규 생성)

```sql
TRUNCATE TABLE wish, option, product, category, member CASCADE;

INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
ALTER SEQUENCE member_id_seq RESTART WITH 3;

INSERT INTO category (id, name) VALUES (1, '식품');
INSERT INTO category (id, name) VALUES (2, '패션');
ALTER SEQUENCE category_id_seq RESTART WITH 3;
```

**핵심 변경 사항:**

1. **`SET REFERENTIAL_INTEGRITY FALSE/TRUE` → `TRUNCATE ... CASCADE`**
   - PostgreSQL의 `TRUNCATE ... CASCADE`는 외래 키로 참조하는 테이블도 함께 비운다
   - 따라서 별도로 참조 무결성을 끄고 켤 필요가 없다

2. **`ALTER TABLE ... ALTER COLUMN id RESTART WITH N` → `ALTER SEQUENCE ... RESTART WITH N`**
   - PostgreSQL에서 `IDENTITY` 컬럼은 내부적으로 시퀀스를 사용한다
   - Hibernate가 `GenerationType.IDENTITY`로 생성한 시퀀스 이름은 `{테이블명}_{컬럼명}_seq` 패턴을 따른다
   - 예: `member` 테이블의 `id` 컬럼 → `member_id_seq`

#### 나머지 SQL 파일 — 그대로 복사

`gift/*.sql`, `wish/*.sql`, `option/*.sql`은 표준 `INSERT INTO` 구문만 사용하므로 H2/PostgreSQL 모두 호환된다. 동일한 내용을 `sql/postgres/` 하위에 같은 디렉토리 구조로 복사한다.

```
src/test/resources/sql/                      ← 기존 (H2, 변경 없음)
├── common-init.sql
├── gift/
│   ├── success.sql
│   ├── exact-quantity.sql
│   ├── insufficient-stock.sql
│   └── zero-stock.sql
├── option/
│   └── success.sql
└── wish/
    ├── success.sql
    └── invalid-member.sql

src/test/resources/sql/postgres/             ← 신규 (PostgreSQL)
├── common-init.sql                          ← PostgreSQL 구문으로 변경
├── gift/
│   ├── success.sql                          ← 동일 (표준 SQL)
│   ├── exact-quantity.sql                   ← 동일
│   ├── insufficient-stock.sql               ← 동일
│   └── zero-stock.sql                       ← 동일
├── option/
│   └── success.sql                          ← 동일
└── wish/
    ├── success.sql                          ← 동일
    └── invalid-member.sql                   ← 동일
```

### 작업 4: Step Definition에서 SQL 경로를 설정으로 분리

현재 Step Definition 코드에서 SQL 경로가 하드코딩되어 있다:

```java
// CommonStepDefinitions.java (현재)
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));

// GiftStepDefinitions.java (현재)
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/gift/success.sql"));
```

이를 Spring 프로퍼티로 설정 가능하게 변경한다:

```java
// 변경 후
@Value("${test.sql.base-path:sql}")
private String sqlBasePath;

// 사용 시
ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlBasePath + "/common-init.sql"));
```

**핵심 포인트: 기존 테스트에 영향 없음**

- `test.sql.base-path`의 기본값이 `sql`이므로 프로파일 없이 실행하면 기존 H2 경로를 사용
- `cucumber` 프로파일이 활성화되면 `application-cucumber.properties`의 `test.sql.base-path=sql/postgres`가 적용

| Gradle 태스크 | 프로파일 | `test.sql.base-path` 값 | SQL 경로 |
|---------------|----------|--------------------------|----------|
| `cucumberTest` | (없음) | `sql` (기본값) | `sql/common-init.sql` (H2) |
| `cucumberPostgresTest` | cucumber | `sql/postgres` | `sql/postgres/common-init.sql` (PostgreSQL) |

#### 수정 대상 Step Definition 파일과 변경 내용

**`CommonStepDefinitions.java`** — `sql/common-init.sql` 경로 1곳

```java
// 변경 전
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));

// 변경 후
@Value("${test.sql.base-path:sql}")
private String sqlBasePath;

ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlBasePath + "/common-init.sql"));
```

**`GiftStepDefinitions.java`** — `sql/common-init.sql` + `sql/gift/*.sql` 경로

```java
// 변경 전
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));
ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/gift/success.sql"));

// 변경 후
@Value("${test.sql.base-path:sql}")
private String sqlBasePath;

ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlBasePath + "/common-init.sql"));
ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlBasePath + "/gift/success.sql"));
```

**`WishStepDefinitions.java`** — `sql/common-init.sql` + `sql/wish/*.sql` 경로

동일 패턴 적용.

### 작업 5: `build.gradle` 수정

#### 5.1 PostgreSQL 드라이버 추가

```groovy
dependencies {
    // ... 기존 의존성 ...
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'  // ← 추가
}
```

#### 5.2 Docker Compose 연동 Gradle 태스크

```groovy
task startDB(type: Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
    doFirst {
        println 'PostgreSQL 컨테이너 시작 중...'
    }
    doLast {
        println 'PostgreSQL 준비 완료'
    }
}

task stopDB(type: Exec) {
    commandLine 'docker', 'compose', 'down'
    doFirst {
        println 'PostgreSQL 컨테이너 종료 중...'
    }
}
```

#### 5.3 `cucumberPostgresTest` 태스크 신규 정의

```groovy
task cucumberPostgresTest(type: Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeEngines 'cucumber'
    }
    systemProperty 'spring.profiles.active', 'cucumber'
    dependsOn startDB
    finalizedBy stopDB
}
```

**기존 `cucumberTest`는 변경하지 않는다:**

```groovy
// 기존 코드 — 그대로 유지
task cucumberTest(type: Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeEngines 'cucumber'
    }
}
```

**`--wait` 플래그:** Docker Compose v2에서 제공하는 기능으로, 모든 서비스의 Health Check가 통과할 때까지 명령이 블로킹된다. 별도의 대기 스크립트 없이도 PostgreSQL 준비를 보장한다.

**실행 흐름:**
```
./gradlew cucumberPostgresTest
  └─→ startDB (docker compose up -d --wait)
       └─→ cucumberPostgresTest 실행 (spring.profiles.active=cucumber)
            └─→ stopDB (docker compose down) ← 성공/실패 무관하게 실행
```

### 작업 6: 테스트 실패 시에도 DB 정리하는 finalizedBy 패턴

작업 5에서 이미 `cucumberPostgresTest`에 `finalizedBy stopDB`를 설정했다. 이 패턴이 중요한 이유를 정리한다.

#### 문제 상황

```
cucumberPostgresTest 실행 → 테스트 실패 → Gradle 빌드 중단 → stopDB 미실행 → 컨테이너 방치
```

방치된 컨테이너는:
- 포트 5432를 점유하여 다음 실행 시 충돌
- 시스템 자원(메모리, CPU) 소비

#### 해결: finalizedBy

```groovy
cucumberPostgresTest.finalizedBy stopDB
```

`finalizedBy`는 Gradle의 태스크 종료자(finalizer)다. 앞선 태스크가 **성공하든 실패하든** 반드시 실행된다.

```
cucumberPostgresTest 성공 → stopDB 실행 ✓
cucumberPostgresTest 실패 → stopDB 실행 ✓
```

#### 주의: dependsOn과의 차이

| 패턴 | 동작 | 실패 시 |
|------|------|---------|
| `A.dependsOn B` | B가 먼저 실행된 후 A 실행 | B 실패 → A 미실행 |
| `A.finalizedBy B` | A 실행 후 B가 반드시 실행 | A 실패해도 B 실행 |

---

## 3부: 변경 대상 파일 목록

### 신규 생성

| 파일 | 내용 |
|------|------|
| `docker-compose.yml` | PostgreSQL 17 서비스 정의, Health Check 포함 |
| `src/test/resources/application-cucumber.properties` | PostgreSQL 연결 설정, `ddl-auto=create-drop`, SQL 경로 설정 |
| `src/test/resources/sql/postgres/common-init.sql` | PostgreSQL 호환 데이터 초기화 스크립트 |
| `src/test/resources/sql/postgres/gift/*.sql` | 기존 `sql/gift/*.sql`과 동일 (표준 SQL) |
| `src/test/resources/sql/postgres/option/success.sql` | 기존과 동일 |
| `src/test/resources/sql/postgres/wish/*.sql` | 기존과 동일 |

### 수정

| 파일 | 변경 내용 | 기존 동작 영향 |
|------|-----------|---------------|
| `build.gradle` | PostgreSQL 드라이버 추가, `startDB`/`stopDB`/`cucumberPostgresTest` 태스크 추가 | 기존 `test`, `cucumberTest` 영향 없음 |
| `CommonStepDefinitions.java` | SQL 경로를 `@Value`로 설정 가능하게 변경 | 기본값 `sql` → 기존과 동일 동작 |
| `GiftStepDefinitions.java` | SQL 경로를 `@Value`로 설정 가능하게 변경 | 기본값 `sql` → 기존과 동일 동작 |
| `WishStepDefinitions.java` | SQL 경로를 `@Value`로 설정 가능하게 변경 | 기본값 `sql` → 기존과 동일 동작 |

### 변경 없음

| 파일 | 이유 |
|------|------|
| `src/main/resources/application.properties` | 기본 프로파일 — 변경 없음 |
| `CucumberSpringConfiguration.java` | 프로파일은 Gradle 시스템 프로퍼티로 주입, 코드 변경 불필요 |
| `src/test/resources/sql/common-init.sql` | 기존 H2 테스트용 — 변경 없음 |
| `src/test/resources/sql/gift/*.sql` | 기존 H2 테스트용 — 변경 없음 |
| `src/test/resources/sql/wish/*.sql` | 기존 H2 테스트용 — 변경 없음 |
| JPA 엔티티 (`model/*.java`) | `GenerationType.IDENTITY`는 PostgreSQL 호환 |
| `Hooks.java`, `ScenarioContext.java` | DB 독립적 코드 |
| Feature 파일 (`*.feature`) | DB 독립적 시나리오 |

### 전체 실행 흐름 비교

```
./gradlew test
  → 단위 테스트 (H2, 변경 없음)

./gradlew cucumberTest
  → 기존 BDD 테스트 (H2, 변경 없음)
  → SQL 경로: sql/ (기본값)

./gradlew cucumberPostgresTest
  → startDB (docker compose up -d --wait)
  → BDD 테스트 (PostgreSQL, spring.profiles.active=cucumber)
  → SQL 경로: sql/postgres/ (프로파일 설정)
  → stopDB (docker compose down)
```

### 실행 순서 요약

```
1. docker-compose.yml 생성
2. application-cucumber.properties 생성
3. sql/postgres/ 디렉토리에 PostgreSQL 호환 SQL 스크립트 생성
4. Step Definition에서 SQL 경로를 @Value로 설정 가능하게 수정
5. build.gradle 수정 (의존성 + Gradle 태스크)
6. ./gradlew cucumberTest 실행하여 기존 테스트 정상 동작 확인
7. ./gradlew cucumberPostgresTest 실행하여 PostgreSQL 연동 검증
```
