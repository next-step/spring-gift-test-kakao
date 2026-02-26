# Application 컨테이너화 가이드

## 개요

요구사항 2에서 PostgreSQL을 Docker 컨테이너로 실행하고, 테스트 JVM 내부의 임베디드 Spring Boot 서버로 테스트했다. 요구사항 3에서는 **Spring Boot 애플리케이션 자체도 Docker 컨테이너로 실행**하여, 테스트가 호스트에서 두 컨테이너(App + DB)를 대상으로 동작하게 한다.

```
요구사항 2 (현재):
  테스트 JVM [Spring Boot 임베디드 서버 + 테스트 코드]
       ↓ JDBC
  Docker [PostgreSQL]

요구사항 3 (목표):
  테스트 JVM [테스트 코드만]
       ↓ HTTP (localhost:28080)        ↓ JDBC (localhost:5432)
  Docker [Spring Boot App]          Docker [PostgreSQL]
       ↓ JDBC (db:5432)                ↑
       └────────────────────────────────┘
         (Docker 내부 네트워크)
```

기존 테스트는 모두 유지하고, 새로운 `cucumberContainerTest` 태스크를 추가한다.

```
./gradlew cucumberTest              ← 기존 유지 (H2, 임베디드 서버)
./gradlew cucumberPostgresTest      ← 기존 유지 (PostgreSQL, 임베디드 서버)
./gradlew cucumberContainerTest     ← 신규 추가 (PostgreSQL + App 컨테이너)
```

---

## 1부: 개념 정리

### 1.1 Multi-stage Build

#### 무엇인가

하나의 Dockerfile에 여러 단계(stage)를 정의하여, **빌드 환경과 실행 환경을 분리**하는 기법이다.

```dockerfile
# Stage 1: Builder — 빌드 도구가 포함된 무거운 이미지
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

# Stage 2: Runtime — 런타임만 포함된 경량 이미지
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 왜 사용하는가

단일 stage로 빌드하면 최종 이미지에 JDK, Gradle, 소스 코드, 빌드 캐시 등이 모두 포함되어 이미지 크기가 커진다.

| 방식 | 이미지 내용 | 크기 (대략) |
|------|------------|------------|
| 단일 stage | JDK + Gradle + 소스 + JAR | ~800MB |
| Multi-stage | JRE + JAR만 | ~200MB |

#### Builder Stage의 역할

빌드에 필요한 **모든 도구**가 포함된 환경이다:
- JDK (컴파일러 포함)
- Gradle (빌드 도구)
- 소스 코드
- 의존성 라이브러리

이 stage에서 `./gradlew bootJar`를 실행하여 실행 가능한 JAR 파일을 생성한다. 빌드 완료 후 이 stage는 **버려진다** — 최종 이미지에 포함되지 않는다.

#### Runtime Stage의 역할

애플리케이션 **실행에 필요한 최소한**만 포함하는 환경이다:
- JRE (실행 환경만, 컴파일러 없음)
- JAR 파일 (Builder stage에서 복사)

`eclipse-temurin:21-jre-alpine`을 사용하는 이유:
- `jre`: JDK가 아닌 JRE만 포함 — 컴파일러, 디버깅 도구 등 제외
- `alpine`: Alpine Linux 기반 — 일반 Linux 이미지(~200MB) 대비 Alpine(~5MB)으로 경량화

#### `COPY --from=builder`

Multi-stage build의 핵심 명령이다. 이전 stage의 파일 시스템에서 파일을 복사한다:

```dockerfile
COPY --from=builder /app/build/libs/*.jar app.jar
```

Builder stage에서 생성된 JAR 파일만 Runtime stage로 가져온다. 소스 코드, Gradle, 빌드 캐시 등은 복사하지 않는다.

### 1.2 Docker 네트워크와 Service Name

#### Service Name이 Hostname이 되는 원리

Docker Compose는 서비스들을 위한 **기본 네트워크를 자동 생성**한다. 이 네트워크 내에서 각 서비스의 이름이 DNS 호스트명으로 등록된다.

```yaml
services:
  db:          # ← 이 이름이 네트워크 내에서 hostname "db"가 된다
    image: postgres:17
  app:         # ← 이 이름이 네트워크 내에서 hostname "app"이 된다
    build: .
```

컨테이너 내부에서 `db`라는 호스트명으로 PostgreSQL에 접근할 수 있다:

```
jdbc:postgresql://db:5432/gift_test
```

이는 Docker의 **내장 DNS 서버**가 서비스 이름을 해당 컨테이너의 IP 주소로 해석하기 때문이다.

#### 왜 `postgres:5432`가 아니라 `db:5432`인가

호스트명은 Docker 이미지 이름이 아니라 **docker-compose.yml의 서비스 이름**이다. 서비스 이름을 `database`로 지정하면 `database:5432`가 된다.

#### 세 가지 관점에서의 네트워크 접근

```
┌──────────────────────────────────────────────────────────┐
│                    호스트 (macOS / Linux)                   │
│                                                            │
│  ┌──────────────┐                                         │
│  │  테스트 (JVM)  │                                        │
│  │               │                                         │
│  │  HTTP ────────┼──→ localhost:28080 ──→ ┌──────────┐    │
│  │               │                        │ App 컨테이너│    │
│  │               │                        │ :8080     │    │
│  │               │                        │           │    │
│  │               │    Docker 내부 네트워크:  │ db:5432 ──┼─┐  │
│  │               │                        └──────────┘ │  │
│  │               │                                      │  │
│  │  JDBC ────────┼──→ localhost:5432 ──→ ┌──────────┐  │  │
│  │               │                        │ DB 컨테이너│←─┘  │
│  │               │                        │ :5432     │    │
│  └──────────────┘                        └──────────┘    │
└──────────────────────────────────────────────────────────┘
```

| 접근 주체 | 대상 | 주소 | 이유 |
|-----------|------|------|------|
| 테스트 (Host) → App | HTTP API | `localhost:28080` | 호스트 포트 매핑 (28080→8080) |
| 테스트 (Host) → DB | JDBC | `localhost:5432` | 호스트 포트 매핑 (5432→5432) |
| App (Container) → DB | JDBC | `db:5432` | Docker 내부 DNS, 포트 매핑 불필요 |

#### 왜 28080인가

호스트에서 이미 8080 포트를 사용 중일 수 있다. 충돌을 피하기 위해 호스트 포트를 28080으로 매핑한다. 컨테이너 내부는 여전히 8080이다:

```yaml
ports:
  - "28080:8080"   # 호스트:컨테이너
```

### 1.3 webEnvironment = NONE

#### 현재 방식 (요구사항 2)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

테스트 JVM 내부에서 **임베디드 Spring Boot 서버**가 시작된다. 테스트 코드와 서버가 같은 JVM에서 실행되며, `@LocalServerPort`로 할당된 포트를 가져온다.

#### 컨테이너 방식 (요구사항 3)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

테스트 JVM에서 **웹 서버를 시작하지 않는다**. Spring ApplicationContext는 생성되지만 (빈 주입, DataSource 등 사용 가능), 임베디드 서버는 없다.

#### 왜 NONE인가

App이 이미 Docker 컨테이너에서 실행 중이므로, 테스트 JVM에서 또 다른 서버를 시작할 필요가 없다. 테스트는 Docker 컨테이너의 앱에 HTTP 요청을 보낸다.

| webEnvironment | 임베디드 서버 | HTTP 대상 | 용도 |
|----------------|-------------|-----------|------|
| `RANDOM_PORT` | 시작됨 | localhost:{랜덤포트} | 임베디드 테스트 |
| `NONE` | 시작 안 됨 | localhost:28080 (컨테이너) | 컨테이너 테스트 |

#### NONE이어도 가능한 것들

`webEnvironment = NONE`이어도 **Spring ApplicationContext는 정상 생성**된다:
- `@Autowired DataSource` — DB 연결 가능 (테스트 데이터 세팅용)
- `@Autowired Repository` — JPA 엔티티 조회 가능 (검증용)
- `@Value` — 프로퍼티 주입 가능

불가능한 것:
- `@LocalServerPort` — 임베디드 서버가 없으므로 사용 불가
- 임베디드 서버 경유 HTTP 요청 — 서버가 없으므로 불가

### 1.4 JdbcTemplate과 데이터 초기화

#### 왜 테스트에서 직접 DB에 접근하는가

컨테이너 테스트에서 테스트 코드는 **호스트 JVM**에서 실행되고, 애플리케이션은 **Docker 컨테이너**에서 실행된다. 두 환경은 완전히 별개의 JVM이다.

테스트 데이터 세팅을 API 호출로만 하면:
- 모든 시나리오마다 복잡한 API 호출 체인이 필요
- 카테고리 생성 → 상품 등록 → 옵션 등록 → 회원 등록 → ... 순서 의존

직접 JDBC로 DB에 접근하면:
- SQL 스크립트 하나로 필요한 데이터를 즉시 세팅
- 기존 `ScriptUtils.executeSqlScript()` 패턴을 그대로 사용 가능

#### 현재 프로젝트에서의 활용

현재 Step Definition들은 이미 `DataSource`와 `ScriptUtils`로 데이터를 세팅한다:

```java
@Autowired
private DataSource dataSource;

try (Connection conn = dataSource.getConnection()) {
    ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlBasePath + "/common-init.sql"));
}
```

`webEnvironment = NONE`이어도 `DataSource`는 주입 가능하므로, 이 패턴은 **그대로 동작**한다. 테스트의 DataSource는 `localhost:5432`로 연결되고, App 컨테이너도 같은 PostgreSQL에 연결되어 있다.

#### JdbcTemplate vs ScriptUtils

| 도구 | 용도 | 특징 |
|------|------|------|
| `ScriptUtils` | SQL 스크립트 파일 실행 | 파일 기반, 여러 문장 한번에 실행 |
| `JdbcTemplate` | 개별 SQL 실행, 결과 조회 | 코드 기반, 유연한 쿼리 |

현재 프로젝트는 ScriptUtils로 데이터를 세팅하고 Repository로 검증한다. 컨테이너 테스트에서도 이 패턴이 유지되므로 **JdbcTemplate 추가 없이도 동작한다**.

JdbcTemplate이 유용해지는 경우:
- JPA 1차 캐시로 인해 Repository 조회가 최신 데이터를 반환하지 않을 때
- 엔티티 매핑 없이 단순 SQL로 검증하고 싶을 때

### 1.5 .dockerignore

#### 왜 필요한가

`docker build` 명령을 실행하면, Docker는 현재 디렉토리(빌드 컨텍스트)의 **모든 파일을 Docker 데몬으로 전송**한다. 불필요한 파일이 많으면:
- 전송 시간이 길어진다
- 빌드 캐시가 불필요하게 무효화된다
- 민감한 파일이 이미지에 포함될 수 있다

`.dockerignore`는 `.gitignore`와 동일한 문법으로 빌드 컨텍스트에서 제외할 파일을 지정한다.

#### 이 프로젝트에서 제외해야 할 것들

```
.gradle/          # Gradle 캐시 (Builder stage에서 다시 다운로드)
build/            # 기존 빌드 결과물 (Builder stage에서 새로 빌드)
.idea/            # IDE 설정
*.md              # 문서
docker-compose*.yml  # Compose 파일
.git/             # Git 히스토리
```

특히 `.gradle/`과 `build/`를 제외하지 않으면 수백 MB가 Docker 데몬으로 전송된다.

### 1.6 Port Mapping

#### 호스트:컨테이너 형식

```yaml
ports:
  - "28080:8080"
```

왼쪽이 호스트 포트, 오른쪽이 컨테이너 포트다.
- **호스트 포트 28080**: 외부(테스트)에서 접근하는 포트
- **컨테이너 포트 8080**: Spring Boot가 컨테이너 내부에서 리슨하는 포트

테스트에서 `RestAssured.port = 28080`으로 설정하면, 요청이 호스트 28080 → 컨테이너 8080으로 전달된다.

### 1.7 depends_on과 컨테이너 시작 순서

#### 기본 depends_on

```yaml
services:
  app:
    depends_on:
      - db
```

이 설정은 `db` 컨테이너가 **시작된 후** `app` 컨테이너를 시작한다. 하지만 "시작됨"은 컨테이너가 생성되었다는 뜻이지, PostgreSQL이 연결을 받을 준비가 되었다는 뜻이 아니다.

#### condition: service_healthy

```yaml
services:
  app:
    depends_on:
      db:
        condition: service_healthy
```

`db` 서비스의 **Health Check가 통과한 후에만** `app` 컨테이너를 시작한다. 이는 PostgreSQL이 실제로 쿼리를 받을 수 있는 상태가 된 후에 Spring Boot가 시작되도록 보장한다.

시작 순서:
```
1. db 컨테이너 시작
2. db Health Check 반복 (pg_isready)
3. db Health Check 통과 (healthy)
4. app 컨테이너 시작
5. app에서 Spring Boot 부팅 → DB 연결 → 테이블 생성
6. app Health Check 통과 (healthy)
7. docker compose up --wait 반환
8. Gradle 테스트 태스크 시작
```

### 1.8 트러블슈팅

컨테이너 환경에서 문제가 발생했을 때 사용하는 핵심 명령들:

#### docker ps — 컨테이너 상태 확인

```bash
docker ps -a
```

실행 중인 컨테이너의 상태(Up, Exited), 포트 매핑, Health 상태를 확인한다. `STATUS` 컬럼에서 `(healthy)`, `(unhealthy)`, `(health: starting)`을 확인할 수 있다.

#### docker logs — 로그 확인

```bash
# App 컨테이너 로그 (Spring Boot 시작 로그, 에러 등)
docker logs <container_name>

# 실시간 로그 추적
docker logs -f <container_name>
```

Spring Boot가 시작에 실패했거나, DB 연결 에러가 발생한 경우 이 로그에서 원인을 파악한다.

#### docker exec — 컨테이너 내부 명령 실행

```bash
# App 컨테이너 내부 셸 접속
docker exec -it <container_name> sh

# DB 컨테이너에서 psql 실행
docker exec -it <db_container_name> psql -U test -d gift_test
```

컨테이너 내부에서 네트워크 연결, 파일 시스템, 환경 변수 등을 직접 확인할 수 있다.

#### docker system prune — 캐시 정리

```bash
# 사용하지 않는 이미지, 컨테이너, 네트워크 정리
docker system prune

# 빌드 캐시까지 포함하여 정리
docker system prune --all
```

빌드가 예상대로 동작하지 않을 때, 캐시된 레이어가 원인일 수 있다. `prune`으로 정리한 후 다시 빌드한다.

---

## 2부: 현재 프로젝트에 적용하기 위한 작업

### 현재 상태

| 항목 | 현재 |
|------|------|
| Docker Compose | `docker-compose.yml` — DB만 정의 |
| Dockerfile | 없음 |
| .dockerignore | 없음 |
| Cucumber 설정 | `CucumberSpringConfiguration` — `RANDOM_PORT` |
| Gradle 태스크 | `cucumberPostgresTest` — DB만 Docker |

### 목표 상태

| 항목 | 목표 |
|------|------|
| Docker Compose | `docker-compose.container.yml` — DB + App 정의 |
| Dockerfile | Multi-stage build (JDK builder → JRE runtime) |
| .dockerignore | 불필요 파일 제외 |
| Cucumber 설정 | `CucumberContainerConfiguration` — `NONE` (별도 클래스) |
| Gradle 태스크 | `cucumberContainerTest` — DB + App 모두 Docker |

**원칙: 기존 코드와 테스트는 건드리지 않는다.**

### 작업 1: Dockerfile 작성 (Multi-stage Build)

프로젝트 루트에 생성한다.

```dockerfile
# Stage 1: Builder
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**설계 결정:**
- Builder: `eclipse-temurin:21-jdk` — Gradle 빌드에 JDK 필요
- Runtime: `eclipse-temurin:21-jre-alpine` — JRE만, Alpine 기반으로 경량화
- `bootJar -x test` — JAR 빌드만 수행, 테스트는 건너뜀 (테스트는 호스트에서 실행)
- `COPY` 순서: `gradlew` → `gradle/` → `src/` 순으로 복사하여 Docker 레이어 캐싱 최적화. 소스 코드가 변경되어도 Gradle Wrapper와 의존성 레이어는 캐시됨.

### 작업 2: .dockerignore 작성

프로젝트 루트에 생성한다.

```
.gradle
build
.idea
*.md
docker-compose*.yml
Dockerfile
.dockerignore
.git
.gitignore
```

**각 항목의 제외 이유:**

| 패턴 | 이유 |
|------|------|
| `.gradle` | Gradle 캐시 — Builder stage에서 새로 다운로드 |
| `build` | 기존 빌드 결과물 — Builder stage에서 새로 빌드 |
| `.idea` | IDE 설정 — 빌드와 무관 |
| `*.md` | 문서 파일 — 런타임에 불필요 |
| `docker-compose*.yml` | Compose 설정 — Docker 빌드와 무관 |
| `.git` | Git 히스토리 — 수십~수백 MB 절약 |

### 작업 3: docker-compose.container.yml 작성

기존 `docker-compose.yml`(DB만)과 별도로, DB + App을 포함하는 파일을 프로젝트 루트에 생성한다.

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

  app:
    build: .
    ports:
      - "28080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gift_test
      SPRING_DATASOURCE_USERNAME: test
      SPRING_DATASOURCE_PASSWORD: test
      SPRING_JPA_HIBERNATE_DDL_AUTO: create
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget --spider -q http://localhost:8080/api/categories || exit 1"]
      interval: 5s
      timeout: 10s
      retries: 10
      start_period: 30s
```

**설계 결정:**

- **별도 파일**: 기존 `docker-compose.yml`(DB만, 요구사항 2용)을 변경하지 않음
- **App 환경 변수**: Spring Boot의 relaxed binding으로 `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- **App → DB 주소**: `db:5432` — Docker 내부 네트워크에서 서비스 이름이 hostname
- **ddl-auto=create**: App이 시작할 때 테이블 생성. 컨테이너 종료 시 데이터는 volume 없이 자동 폐기
- **App Health Check**: `wget --spider`로 API 엔드포인트 접근 확인. Alpine 이미지에 `wget` 포함
- **start_period: 30s**: Spring Boot 시작에 시간이 걸리므로 넉넉히 설정
- **depends_on condition**: DB가 healthy가 된 후에만 App 시작

#### 컨테이너 시작 순서

```
docker compose -f docker-compose.container.yml up -d --wait
  1. db 컨테이너 시작
  2. db Health Check 통과 (pg_isready)
  3. app 컨테이너 시작 (depends_on: db healthy)
  4. app에서 Gradle build → Spring Boot 부팅 → 테이블 생성
  5. app Health Check 통과 (wget → /api/categories 응답)
  6. --wait 반환 → 테스트 시작
```

### 작업 4: application-container.properties 생성

`src/test/resources/application-container.properties`에 생성한다.

```properties
# 테스트 JVM에서 PostgreSQL에 직접 연결 (데이터 세팅/검증용)
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver

# 테이블은 App 컨테이너가 생성 — 테스트에서는 스키마를 건드리지 않음
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# PostgreSQL 호환 SQL 스크립트 경로
test.sql.base-path=sql/postgres
```

**`cucumber` 프로파일과의 차이:**

| 설정 | cucumber (요구사항 2) | container (요구사항 3) |
|------|----------------------|----------------------|
| `ddl-auto` | `create-drop` | `validate` |
| 이유 | 테스트 JVM이 스키마 관리 | App 컨테이너가 스키마 관리 |

`validate`를 사용하면 Hibernate가 엔티티와 실제 테이블 구조가 일치하는지 검증만 한다. 테이블 생성/삭제는 하지 않는다.

### 작업 5: Cucumber 컨테이너 설정 클래스 생성

기존 `gift.cucumber` 패키지와 별도로, `gift.container` 패키지에 생성한다.

#### 패키지 분리 이유

Cucumber JUnit Platform Engine은 glue 경로의 `@CucumberContextConfiguration`을 찾는다. 기존 설정(`gift.cucumber`)과 새 설정을 같은 패키지 트리에 두면, 기본 glue 스캔 시 두 설정이 동시에 발견되어 충돌한다.

`gift.container`는 `gift.cucumber`의 하위 패키지가 아니므로, 기존 테스트의 기본 glue 스캔(`gift.cucumber` + 하위 패키지)에서 발견되지 않는다.

```
src/test/java/gift/
├── cucumber/                          ← 기존 (변경 없음)
│   ├── CucumberSpringConfiguration.java   (RANDOM_PORT)
│   ├── Hooks.java                         (@LocalServerPort)
│   ├── ScenarioContext.java
│   └── stepdefs/                          (Step Definition들)
└── container/                         ← 신규
    ├── CucumberContainerConfiguration.java (NONE)
    └── ContainerHooks.java                (고정 포트 28080)
```

#### CucumberContainerConfiguration.java

```java
package gift.container;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("container")
public class CucumberContainerConfiguration {
}
```

**기존 `CucumberSpringConfiguration`과의 차이:**

| 항목 | 기존 | 컨테이너 |
|------|------|---------|
| `webEnvironment` | `RANDOM_PORT` | `NONE` |
| `@ActiveProfiles` | 없음 (Gradle에서 주입) | `"container"` |
| 임베디드 서버 | 시작됨 | 시작 안 됨 |

여기서는 `@ActiveProfiles`를 직접 사용한다. 이 설정 클래스는 컨테이너 테스트 전용이므로, 프로파일을 코드에 명시하는 것이 의도를 명확히 한다.

#### ContainerHooks.java

```java
package gift.container;

import io.cucumber.java.Before;
import io.restassured.RestAssured;

public class ContainerHooks {

    @Before
    public void setUp() {
        RestAssured.port = 28080;
    }
}
```

**기존 `Hooks`와의 차이:**

| 항목 | 기존 Hooks | ContainerHooks |
|------|-----------|---------------|
| 포트 결정 | `@LocalServerPort` (랜덤) | 고정 `28080` |
| 이유 | 임베디드 서버 포트 | Docker 포트 매핑 |

`@LocalServerPort`는 `webEnvironment = NONE`에서 사용할 수 없으므로, Docker Compose에서 매핑한 포트(28080)를 직접 지정한다.

### 작업 6: build.gradle 수정

#### 컨테이너 Docker Compose 태스크

```groovy
task startContainers(type: Exec) {
    commandLine 'docker', 'compose', '-f', 'docker-compose.container.yml', 'up', '-d', '--wait', '--build'
    doFirst {
        println 'App + PostgreSQL 컨테이너 시작 중...'
    }
    doLast {
        println '컨테이너 준비 완료'
    }
}

task stopContainers(type: Exec) {
    commandLine 'docker', 'compose', '-f', 'docker-compose.container.yml', 'down'
    doFirst {
        println '컨테이너 종료 중...'
    }
}
```

**`-f docker-compose.container.yml`**: 기본 `docker-compose.yml` 대신 컨테이너 전용 파일 사용
**`--build`**: 매 실행마다 App 이미지를 다시 빌드. 소스 코드 변경이 반영되도록 보장

#### cucumberContainerTest 태스크

```groovy
task cucumberContainerTest(type: Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeEngines 'cucumber'
    }
    systemProperty 'cucumber.glue', 'gift.container,gift.cucumber.stepdefs'
    dependsOn startContainers
    finalizedBy stopContainers
}
```

**`cucumber.glue` 설정이 핵심이다:**

| glue 경로 | 포함되는 클래스 |
|-----------|---------------|
| `gift.container` | `CucumberContainerConfiguration`, `ContainerHooks` |
| `gift.cucumber.stepdefs` | 기존 Step Definition 전체 (재사용) |

기존 `gift.cucumber.CucumberSpringConfiguration`과 `gift.cucumber.Hooks`는 glue 경로에 포함되지 않으므로 충돌 없음.

**기존 태스크는 변경하지 않는다:**

기존 `cucumberTest`와 `cucumberPostgresTest`는 glue를 명시하지 않는다. 기본 동작으로 `@CucumberContextConfiguration`이 있는 `gift.cucumber` 패키지와 하위 패키지를 스캔한다. `gift.container`는 `gift.cucumber`의 하위가 아니므로 발견되지 않는다.

### 실행 흐름

```
./gradlew cucumberContainerTest
  │
  ├─→ startContainers
  │     docker compose -f docker-compose.container.yml up -d --wait --build
  │       1. App 이미지 빌드 (Multi-stage)
  │       2. db 컨테이너 시작 → Health Check 통과
  │       3. app 컨테이너 시작 → Health Check 통과
  │       4. --wait 반환
  │
  ├─→ cucumberContainerTest 실행
  │     - Spring Context 시작 (webEnvironment=NONE, profile=container)
  │     - DataSource → localhost:5432 (Host → DB 컨테이너)
  │     - RestAssured → localhost:28080 (Host → App 컨테이너)
  │     - Step Definitions 실행 (기존 코드 재사용)
  │
  └─→ stopContainers (finalizedBy — 성공/실패 무관)
        docker compose -f docker-compose.container.yml down
```

---

## 3부: 변경 대상 파일 목록

### 신규 생성

| 파일 | 내용 |
|------|------|
| `Dockerfile` | Multi-stage build (JDK builder → JRE Alpine runtime) |
| `.dockerignore` | 빌드 컨텍스트에서 불필요 파일 제외 |
| `docker-compose.container.yml` | DB + App 서비스 정의, Health Check, depends_on |
| `src/test/resources/application-container.properties` | 테스트 DataSource (localhost:5432), ddl-auto=validate |
| `src/test/java/gift/container/CucumberContainerConfiguration.java` | webEnvironment=NONE, @ActiveProfiles("container") |
| `src/test/java/gift/container/ContainerHooks.java` | RestAssured.port = 28080 |

### 수정

| 파일 | 변경 내용 | 기존 동작 영향 |
|------|-----------|---------------|
| `build.gradle` | `startContainers`/`stopContainers`/`cucumberContainerTest` 태스크 추가 | 기존 태스크 영향 없음 |

### 변경 없음

| 파일 | 이유 |
|------|------|
| `docker-compose.yml` | 요구사항 2 전용 — 변경 없음 |
| `CucumberSpringConfiguration.java` | 기존 RANDOM_PORT 테스트 전용 — 변경 없음 |
| `Hooks.java` | 기존 @LocalServerPort 설정 — 변경 없음 |
| `application-cucumber.properties` | 요구사항 2 전용 — 변경 없음 |
| Step Definition 파일들 | `cucumber.glue`로 재사용 — 코드 변경 없음 |
| `sql/postgres/*.sql` | 동일 PostgreSQL — 재사용 |
| Feature 파일 (`*.feature`) | DB/인프라 독립적 — 변경 없음 |
| JPA 엔티티, 서비스, 컨트롤러 | 애플리케이션 코드 변경 없음 |

### 전체 Gradle 태스크 비교

```
./gradlew test
  → 단위 테스트 (H2, 임베디드 서버)

./gradlew cucumberTest
  → BDD 테스트 (H2, 임베디드 서버)

./gradlew cucumberPostgresTest
  → BDD 테스트 (Docker PostgreSQL, 임베디드 서버)
  → Docker: db만

./gradlew cucumberContainerTest
  → BDD 테스트 (Docker PostgreSQL + Docker App)
  → Docker: db + app
  → 테스트 JVM에 임베디드 서버 없음 (webEnvironment=NONE)
```

### 실행 순서 요약

```
1. Dockerfile 작성 (Multi-stage build)
2. .dockerignore 작성
3. docker-compose.container.yml 작성
4. application-container.properties 생성
5. CucumberContainerConfiguration + ContainerHooks 생성
6. build.gradle 수정 (Gradle 태스크 추가)
7. ./gradlew cucumberTest 실행하여 기존 테스트 정상 동작 확인
8. ./gradlew cucumberPostgresTest 실행하여 기존 PostgreSQL 테스트 정상 동작 확인
9. ./gradlew cucumberContainerTest 실행하여 컨테이너 연동 검증
```
