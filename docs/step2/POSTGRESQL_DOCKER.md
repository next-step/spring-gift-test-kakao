# 요구사항 2: PostgreSQL + Docker Compose 통합 — 구현 가이드

## 개요

H2 in-memory DB를 **PostgreSQL**로 전환하고, **Docker Compose**로 테스트 환경을 자동화했다.
핵심 목표는 **프로덕션과 동일한 DB에서 테스트**하면서, **한 줄 명령으로 환경 준비부터 정리까지 자동화**하는 것이다.

### Before vs After

**Before** — H2 in-memory (개발 편의, 프로덕션과 다름)

```
./gradlew test
  → H2 자동 시작 (JVM 내장) → 테스트 실행 → 종료
  → PostgreSQL 고유 문법/동작은 검증 불가
```

**After** — PostgreSQL in Docker (프로덕션 동일)

```
./gradlew cucumberTest
  → Docker Compose로 PostgreSQL 컨테이너 시작
  → Health check로 준비 완료 대기
  → Cucumber 테스트 실행
  → 테스트 성공/실패 무관하게 컨테이너 정리
```

---

## Production Parity란?

**Production Parity**는 개발/테스트/프로덕션 환경을 **최대한 동일하게** 유지하는 원칙이다.
[The Twelve-Factor App](https://12factor.net/dev-prod-parity)에서 강조하는 핵심 요소 중 하나다.

### H2 vs PostgreSQL 차이 사례

| 항목 | H2 | PostgreSQL |
|------|-----|-----------|
| 문자열 비교 | 대소문자 구분 안 함 (기본) | 대소문자 구분 |
| `GENERATED ALWAYS` | 지원 안 함 | 지원 |
| JSON 타입 | 제한적 | `jsonb` 네이티브 지원 |
| 트랜잭션 격리 | 단순화된 구현 | MVCC 기반 완전 구현 |

H2에서 통과한 테스트가 PostgreSQL에서 실패하는 경우가 실제로 발생한다.
프로덕션에 PostgreSQL을 쓴다면, 테스트도 PostgreSQL에서 실행해야 이런 문제를 잡을 수 있다.

---

## 전체 아키텍처

```
Host (로컬 머신)
│
├── ./gradlew cucumberTest
│     ├── dockerUp (docker-compose up -d --wait)
│     ├── Cucumber 테스트 실행
│     └── dockerDown (docker-compose down)
│
└── 테스트 코드 ──JDBC──→ localhost:5432 ──→ PostgreSQL (Container)
```

요구사항 2에서는 **테스트 코드와 Spring 앱이 모두 호스트**에서 실행된다.
Docker는 **PostgreSQL만** 실행한다.

---

## 변경된 파일 구조

```
프로젝트 루트/
├── docker-compose.yml                          ← PostgreSQL 서비스 정의
├── build.gradle                                ← Gradle 태스크 추가, PG 드라이버
└── src/test/resources/
    └── application-cucumber.properties         ← PostgreSQL 접속 설정
```

---

## 구현 단계별 상세 설명

### 1단계: Docker Compose 설정 (`docker-compose.yml`)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: gift
      POSTGRES_PASSWORD: gift
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gift -d gift_test"]
      interval: 3s
      timeout: 3s
      retries: 10
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

#### 각 설정의 의미

| 설정 | 역할 |
|------|------|
| `image: postgres:16-alpine` | PostgreSQL 16 경량 이미지. Alpine Linux 기반으로 용량이 작다 |
| `ports: "5432:5432"` | 호스트의 5432 포트를 컨테이너의 5432 포트에 매핑. 호스트에서 `localhost:5432`로 접근 가능 |
| `POSTGRES_DB` | 컨테이너 시작 시 자동 생성할 데이터베이스 이름 |
| `POSTGRES_USER/PASSWORD` | DB 접속 계정. Spring의 `spring.datasource.username/password`와 일치해야 한다 |
| `healthcheck` | 컨테이너가 "준비됨" 상태인지 판단하는 기준 |
| `volumes: pgdata` | 데이터를 Named Volume에 저장. 컨테이너 재시작 시 데이터 유지 |

#### Health Check가 왜 필요한가?

Docker 컨테이너가 "시작됨(running)"과 "준비됨(healthy)"은 다르다.

```
컨테이너 시작됨 (running)     ← PostgreSQL 프로세스 기동 중
  ↓ (약 1~3초)
PostgreSQL 준비됨 (healthy)   ← 쿼리 수락 가능
```

Health check 없이 바로 테스트를 실행하면, PostgreSQL이 아직 쿼리를 받을 수 없는 상태에서
JDBC 연결을 시도하여 `Connection refused` 에러가 발생한다.

`pg_isready`는 PostgreSQL이 제공하는 유틸리티로, 서버가 연결을 수락할 준비가 되었는지 확인한다.

---

### 2단계: Spring 프로파일 설정 (`application-cucumber.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=gift
spring.datasource.password=gift
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
```

#### 프로파일 동작 원리

Spring Boot에서 `application-{profile}.properties` 파일은 해당 프로파일이 활성화될 때만 로드된다.

```
./gradlew test (기본 프로파일)
  → application.properties 로드
  → H2 in-memory DB 사용 (Spring Boot 자동 설정)

./gradlew cucumberTest (cucumber 프로파일)
  → application.properties + application-cucumber.properties 로드
  → PostgreSQL 접속 정보가 H2 설정을 오버라이드
```

`CucumberSpringConfig`의 `@ActiveProfiles("cucumber")`가 프로파일을 활성화한다:

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")
public class CucumberSpringConfig {
}
```

#### 각 설정의 의미

| 설정 | 역할 |
|------|------|
| `spring.datasource.url` | JDBC 접속 URL. `localhost:5432`는 Docker가 매핑한 포트 |
| `driver-class-name` | PostgreSQL JDBC 드라이버 클래스 |
| `database-platform` | Hibernate가 PostgreSQL에 맞는 SQL을 생성하도록 지정 |
| `ddl-auto=create-drop` | 앱 시작 시 스키마 생성, 종료 시 삭제. 매 테스트 실행마다 깨끗한 스키마 보장 |

#### `ddl-auto` 옵션 비교

| 값 | 동작 | 사용 시점 |
|-----|------|----------|
| `none` | 아무것도 안 함 | 스키마가 이미 존재할 때 |
| `validate` | 엔티티와 스키마 일치 여부만 검증 | 프로덕션 |
| `create` | 기존 테이블 DROP 후 CREATE | 테스트 |
| `create-drop` | create + 앱 종료 시 DROP | 테스트 (가장 깨끗) |
| `update` | 변경분만 ALTER | 개발 (위험할 수 있음) |

---

### 3단계: Gradle 의존성 및 태스크 (`build.gradle`)

#### PostgreSQL 드라이버 추가

```groovy
testRuntimeOnly 'org.postgresql:postgresql'
```

`testRuntimeOnly`인 이유: 컴파일 시에는 JDBC 인터페이스만 사용하고,
실행 시에만 PostgreSQL 드라이버 구현체가 필요하다.
또한 main 코드의 기본 H2 설정에 영향을 주지 않기 위해 `test` 스코프로 한정했다.

#### Docker 라이프사이클 태스크

```groovy
tasks.register('dockerUp', Exec) {
    commandLine 'docker-compose', 'up', '-d', '--wait'
}

tasks.register('dockerDown', Exec) {
    commandLine 'docker-compose', 'down'
}

tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    include 'gift/cucumber/**'
    dependsOn 'dockerUp'
    finalizedBy 'dockerDown'
}
```

#### 태스크 실행 흐름

```
./gradlew cucumberTest
  │
  ├── 1. dockerUp 실행 (dependsOn)
  │     └── docker-compose up -d --wait
  │         ├── PostgreSQL 컨테이너 시작
  │         └── healthcheck 통과까지 대기
  │
  ├── 2. cucumberTest 실행
  │     └── Cucumber 시나리오 6개 실행
  │
  └── 3. dockerDown 실행 (finalizedBy)
        └── docker-compose down
            └── 컨테이너 + 네트워크 정리
```

#### `dependsOn` vs `finalizedBy`

| 키워드 | 의미 | 특징 |
|--------|------|------|
| `dependsOn` | "이 태스크 실행 **전에** 먼저 실행" | 선행 태스크 실패 시 본 태스크 미실행 |
| `finalizedBy` | "이 태스크 실행 **후에** 반드시 실행" | 본 태스크 성공/실패 **무관**하게 실행 |

`finalizedBy`가 핵심이다. 테스트가 실패하더라도 `dockerDown`이 반드시 실행되어
PostgreSQL 컨테이너가 정리된다. 이것이 없으면 테스트 실패 시 컨테이너가 남아
포트 충돌이 발생할 수 있다.

#### `docker-compose up` 플래그

| 플래그 | 역할 |
|--------|------|
| `-d` (detach) | 백그라운드 실행. 없으면 로그가 포그라운드에 출력되어 Gradle이 블록된다 |
| `--wait` | 모든 서비스의 healthcheck가 통과할 때까지 대기. 이 플래그가 핵심 |

---

### 4단계: 테스트 파이프라인 분리

H2 기반 테스트와 PostgreSQL 기반 Cucumber 테스트를 **독립된 파이프라인**으로 분리했다.

```groovy
// H2 기반: 단위 테스트 + RestAssured 인수 테스트
tasks.named('test') {
    useJUnitPlatform()
    exclude 'gift/cucumber/**'   // Cucumber 테스트 제외
}

// PostgreSQL 기반: Cucumber BDD 테스트
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    include 'gift/cucumber/**'   // Cucumber 테스트만 포함
    dependsOn 'dockerUp'
    finalizedBy 'dockerDown'
}
```

#### 왜 분리하는가?

| 파이프라인 | 명령 | DB | Docker 필요 | 속도 |
|-----------|------|-----|------------|------|
| `test` | `./gradlew test` | H2 (in-memory) | 불필요 | 빠름 (~4초) |
| `cucumberTest` | `./gradlew cucumberTest` | PostgreSQL (Docker) | 필요 | 느림 (~15초) |

- **빠른 피드백**: 로직 변경 시 `./gradlew test`로 즉시 확인 (Docker 대기 없음)
- **정확한 검증**: DB 관련 변경 시 `./gradlew cucumberTest`로 실제 환경 검증
- **CI 유연성**: CI에서 두 파이프라인을 병렬 실행 가능

---

## 데이터 격리 전략

PostgreSQL 환경에서도 시나리오 간 데이터 독립성을 유지하는 구조는 요구사항 1과 동일하다.

```
시나리오 실행 전
  ├── DatabaseCleanupHook (@Before)
  │     └── deleteAllInBatch() (wish → option → product → category → member)
  └── TestContext (@ScenarioScope)
        └── 새 인스턴스 생성

시나리오 실행
  ├── Given: Repository로 직접 시드 (PostgreSQL에 INSERT)
  ├── When: RestAssured로 HTTP 요청
  └── Then: 응답 검증

시나리오 종료
  └── TestContext 인스턴스 폐기
```

H2에서 PostgreSQL로 전환해도 `DatabaseCleanupHook`의 코드가 변경되지 않는 이유:
Spring Data JPA의 `Repository.deleteAllInBatch()`가 DB 벤더에 독립적이기 때문이다.

---

## 네트워크 이해

```
┌──────────────────────────────────────────┐
│  Host (로컬 머신)                          │
│                                           │
│  Spring Boot 앱  ──┐                      │
│  Cucumber 테스트 ──┤──JDBC──→ localhost:5432
│                    │                      │
└────────────────────┼──────────────────────┘
                     │
        ┌────────────▼──────────────┐
        │  Docker: PostgreSQL       │
        │  (내부 포트 5432)          │
        │                           │
        │  POSTGRES_DB: gift_test   │
        │  POSTGRES_USER: gift      │
        └───────────────────────────┘
```

- 호스트의 `localhost:5432`가 컨테이너의 `5432`로 포워딩된다 (포트 매핑).
- Spring Boot 앱과 테스트 코드가 **같은 JVM**에서 실행되므로, 동일한 JDBC URL을 사용한다.

---

## 실행 결과

```bash
# PostgreSQL 기반 Cucumber 테스트
./gradlew cucumberTest
# → dockerUp: PostgreSQL 컨테이너 시작 + healthcheck 대기
# → cucumberTest: 6개 시나리오 통과
# → dockerDown: 컨테이너 정리

# H2 기반 단위 테스트 (Docker 불필요, 영향 없음)
./gradlew test
# → 8개 테스트 통과 (OptionTest 2 + ApiTest 6)
```

---

## 의사결정 기록

| 결정 | 이유 |
|------|------|
| PostgreSQL 16 Alpine 사용 | 최신 안정 버전 + 경량 이미지 |
| `pg_isready`로 healthcheck | PostgreSQL 공식 유틸리티, 가장 신뢰할 수 있는 준비 상태 확인 방법 |
| Named Volume (`pgdata`) 사용 | 컨테이너 재시작 시 데이터 유지, 개발 시 편의성 |
| `testRuntimeOnly` 스코프 | main 코드의 H2 기본 설정에 영향 없이 테스트에서만 PG 드라이버 사용 |
| `finalizedBy 'dockerDown'` | 테스트 실패 시에도 반드시 컨테이너 정리 |
| `ddl-auto=create-drop` | 매 실행마다 깨끗한 스키마 보장 |
| `test`와 `cucumberTest` 분리 | 빠른 피드백(H2)과 정확한 검증(PG)을 모두 확보 |
| `--wait` 플래그 사용 | healthcheck 통과까지 Gradle이 대기, 타이밍 이슈 방지 |
