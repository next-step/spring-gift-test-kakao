# 요구사항 3: Application 컨테이너화 — 구현 가이드

## 개요

Spring Boot 앱 자체를 **Docker 컨테이너로 실행**하고, 테스트는 호스트에서
Docker 컨테이너의 앱에 **HTTP 요청을 보내는 E2E 구조**로 전환했다.
핵심 목표는 **프로덕션 배포와 완전히 동일한 환경**에서 테스트하는 것이다.

### Before vs After

**Before** (요구사항 2) — 앱은 호스트, DB만 Docker

```
Host: Spring Boot 앱 + 테스트 코드
Docker: PostgreSQL만
```

**After** (요구사항 3) — 앱도 Docker, 테스트만 호스트

```
Host: 테스트 코드만 (HTTP 요청 전송 + DB 시드/정리)
Docker: Spring Boot 앱 + PostgreSQL
```

이 전환으로 "내 PC에서는 되는데 서버에서 안 돼요" 문제를 근본적으로 차단한다.
Docker 이미지가 곧 배포 단위이므로, 테스트를 통과한 이미지를 그대로 배포할 수 있다.

---

## 전체 아키텍처

```
┌──────────────────────────────────────────────────────┐
│  Host (로컬 머신)                                      │
│                                                       │
│  테스트 코드 ──HTTP──→ localhost:28080                  │
│  테스트 코드 ──JDBC──→ localhost:5432                   │
└───────────────────┬─────────────────┬─────────────────┘
                    │                 │
    ┌───────────────▼──┐   ┌─────────▼────────────┐
    │  Docker: App      │   │  Docker: PostgreSQL   │
    │  (port 8080)      │   │  (port 5432)          │
    │                   │   │                       │
    │  ──JDBC──→ postgres:5432 ◄────────────────── │
    └───────────────────┘   └──────────────────────┘
          Docker 내부 네트워크
```

### 네트워크 경로 정리

| 출발 | 도착 | 프로토콜 | 주소 |
|------|------|---------|------|
| 테스트 코드 (Host) | Spring Boot 앱 (Docker) | HTTP | `localhost:28080` |
| 테스트 코드 (Host) | PostgreSQL (Docker) | JDBC | `localhost:5432` |
| Spring Boot 앱 (Docker) | PostgreSQL (Docker) | JDBC | `postgres:5432` |

- **호스트 → 컨테이너**: 포트 매핑(`28080:8080`, `5432:5432`)으로 접근
- **컨테이너 → 컨테이너**: Docker 내부 네트워크에서 **서비스명**이 호스트명으로 사용됨

#### 왜 테스트에서 DB에 직접 접속하는가?

테스트 코드가 DB에 JDBC로 직접 연결하는 이유는 **데이터 시드와 정리** 때문이다:

- `@Before` hook에서 `Repository.deleteAllInBatch()`로 DB 정리
- Given step에서 `Repository.save()`로 테스트 데이터 시드

이 작업을 HTTP API로 하면, API 자체의 버그가 테스트 데이터 준비를 실패시킬 수 있다.
DB 직접 접근으로 **테스트 인프라와 테스트 대상을 분리**한다.

---

## 변경된 파일 구조

```
프로젝트 루트/
├── .dockerignore                                ← Docker 빌드 컨텍스트 제외 목록
├── Dockerfile                                   ← Multi-stage 빌드
├── docker-compose.yml                           ← app 서비스 추가
├── build.gradle                                 ← runtimeOnly PG, dockerBuild 태스크
└── src/test/
    ├── java/gift/cucumber/
    │   ├── CucumberSpringConfig.java            ← webEnvironment=NONE
    │   └── steps/
    │       ├── CategorySteps.java               ← baseUri(targetUrl)
    │       ├── ProductSteps.java                ← baseUri(targetUrl)
    │       └── GiftSteps.java                   ← baseUri(targetUrl)
    └── resources/
        └── application-cucumber.properties      ← validate + target URL
```

---

## 구현 단계별 상세 설명

### 1단계: `.dockerignore` 생성

```
.git
.gradle
.idea
build
*.iml
.DS_Store
docs
.claude
```

Docker 이미지를 빌드할 때, Docker 데몬에 전송되는 **빌드 컨텍스트**에서 불필요한 파일을 제외한다.
`.git` 디렉토리만 해도 수십 MB가 될 수 있으므로, 빌드 속도와 이미지 크기에 직접적인 영향을 준다.

---

### 2단계: Dockerfile 작성 (Multi-stage Build)

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Multi-stage Build란?

하나의 Dockerfile에서 **여러 단계(stage)** 를 정의하고, 최종 이미지에는
필요한 결과물만 복사하는 기법이다.

```
Stage 1 (builder): JDK + Gradle + 소스코드 → JAR 빌드 (~500MB)
                          │
                    COPY --from=builder
                          │
Stage 2 (runtime): JRE + app.jar만 포함 (~200MB)
```

**Builder stage**에서 사용한 JDK, Gradle, 소스코드는 최종 이미지에 포함되지 않는다.
결과적으로 이미지 크기가 60% 이상 줄어든다.

#### 의존성 레이어 캐싱

```dockerfile
# 먼저 빌드 설정 파일만 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true   ← 의존성 다운로드

# 그 다음 소스 코드 복사
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test         ← JAR 빌드
```

Docker는 각 명령어의 결과를 **레이어로 캐싱**한다.
소스 코드가 변경되더라도 `build.gradle`이 변경되지 않았다면,
의존성 다운로드 레이어가 캐시에서 재사용된다. 빌드 시간이 크게 단축된다.

#### 왜 builder는 jammy, runtime은 alpine인가?

| Stage | 베이스 이미지 | 이유 |
|-------|-------------|------|
| builder | `eclipse-temurin:21-jdk-jammy` | Ubuntu(glibc) 기반. Gradle의 네이티브 라이브러리가 Alpine(musl)에서 ARM64 SIGSEGV 발생 |
| runtime | `eclipse-temurin:21-jre-alpine` | 경량 이미지 (~200MB). JAR 실행에는 네이티브 호환성 문제 없음 |

Alpine Linux는 표준 C 라이브러리로 `musl`을 사용하는데,
Gradle의 `libnative-platform-file-events.so`가 `glibc`에 의존하여
ARM64(Apple Silicon) 환경에서 충돌이 발생한다.
런타임에서는 Gradle을 사용하지 않으므로 Alpine이 안전하다.

---

### 3단계: Docker Compose에 `app` 서비스 추가

```yaml
  app:
    build: .
    ports:
      - "28080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
      SPRING_DATASOURCE_USERNAME: gift
      SPRING_DATASOURCE_PASSWORD: gift
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
      SPRING_JPA_HIBERNATE_DDL_AUTO: create
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget --quiet --spider http://localhost:8080/api/categories || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 30s
```

#### 환경변수로 Spring 설정 주입

Spring Boot는 **환경변수를 properties로 자동 매핑**한다:

```
환경변수: SPRING_DATASOURCE_URL
   ↓ (Spring Boot Relaxed Binding)
properties: spring.datasource.url
```

이 메커니즘 덕분에 `src/main/` 코드를 전혀 수정하지 않고도
Docker 환경에 맞는 설정을 주입할 수 있다.

#### 포트 매핑: 왜 28080인가?

```
호스트 28080 → 컨테이너 8080
```

- 컨테이너 내부에서는 Spring Boot 기본 포트 `8080`을 사용한다.
- 호스트에서는 `28080`으로 매핑하여, 로컬에서 실행 중인 다른 앱과 충돌을 방지한다.
- 테스트 코드는 `localhost:28080`으로 HTTP 요청을 보낸다.

#### `depends_on`과 `healthcheck`의 관계

```yaml
depends_on:
  postgres:
    condition: service_healthy
```

이 설정은 PostgreSQL의 healthcheck가 "healthy"가 될 때까지 app 시작을 **대기**시킨다.

```
1. postgres 시작
2. pg_isready 성공 → postgres healthy
3. app 시작 (PostgreSQL이 준비된 후)
4. wget 성공 → app healthy
5. docker-compose --wait 완료 → Gradle 테스트 실행
```

app의 healthcheck는 `wget`으로 실제 API 엔드포인트를 호출한다.
Alpine 이미지에는 `curl`이 없지만 `wget`은 기본 포함되어 있다.
`start_period: 30s`는 Spring Boot 기동 시간을 고려한 대기 시간이다.

#### `ddl-auto`의 역할 분담

| 컴포넌트 | `ddl-auto` | 역할 |
|---------|-----------|------|
| Docker App | `create` | **스키마 생성** 담당. 앱 시작 시 테이블 생성 |
| 테스트 코드 | `validate` | 스키마 **검증만**. 엔티티와 테이블 일치 여부 확인 |

앱이 스키마를 생성하고, 테스트는 그 스키마가 올바른지만 확인한다.
두 곳에서 모두 `create`를 사용하면 테스트 시작 시 앱이 만든 스키마가 드랍될 수 있다.

---

### 4단계: `build.gradle` 수정

#### PostgreSQL 드라이버 스코프 변경

```groovy
# 변경 전
testRuntimeOnly 'org.postgresql:postgresql'

# 변경 후
runtimeOnly 'org.postgresql:postgresql'
```

요구사항 2에서는 `testRuntimeOnly`로 충분했다 (테스트에서만 PostgreSQL 사용).
요구사항 3에서는 Docker 컨테이너의 앱이 PostgreSQL에 접속해야 하므로,
**bootJar에 드라이버가 포함**되어야 한다. `runtimeOnly`로 변경하면 main JAR에 포함된다.

#### `dockerBuild` 태스크 추가

```groovy
tasks.register('dockerBuild', Exec) {
    commandLine 'docker-compose', 'build'
}
```

Docker 이미지 빌드를 Gradle 태스크로 래핑하여 일관된 인터페이스를 제공한다.

---

### 5단계: `CucumberSpringConfig` 수정

```java
// 변경 전
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

// 변경 후
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

#### `RANDOM_PORT` vs `NONE`

| 모드 | 동작 | 사용 시점 |
|------|------|----------|
| `RANDOM_PORT` | 내장 톰캣을 랜덤 포트로 기동 | 앱이 호스트에서 실행될 때 (요구사항 2) |
| `NONE` | 웹서버 **미기동** | 앱이 Docker에서 실행될 때 (요구사항 3) |

요구사항 3에서 앱은 Docker 컨테이너에서 실행된다.
테스트의 Spring context는 **Repository 접근(데이터 시드/정리)** 만 필요하므로
웹서버를 띄울 이유가 없다. `NONE`으로 변경하면 테스트 기동 시간도 단축된다.

---

### 6단계: `application-cucumber.properties` 수정

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=gift
spring.datasource.password=gift
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
cucumber.target.url=http://localhost:28080
```

#### 변경 사항

| 항목 | 변경 전 | 변경 후 | 이유 |
|------|---------|---------|------|
| `ddl-auto` | `create-drop` | `validate` | 스키마는 Docker 앱이 생성, 테스트는 검증만 |
| `cucumber.target.url` | (없음) | `http://localhost:28080` | RestAssured가 Docker 앱에 요청을 보낼 URL |

`cucumber.target.url`은 Spring Boot의 표준 속성이 아니라 **커스텀 속성**이다.
Step Definition에서 `@Value("${cucumber.target.url}")`로 주입받아 사용한다.

---

### 7단계: Step Definitions 수정

3개 파일(`CategorySteps`, `ProductSteps`, `GiftSteps`) 모두 동일한 패턴으로 변경했다.

```java
// 변경 전
@LocalServerPort
private int port;

RestAssured.given()
    .port(port)
    ...

// 변경 후
@Value("${cucumber.target.url}")
private String targetUrl;

RestAssured.given()
    .baseUri(targetUrl)
    ...
```

#### 왜 `@LocalServerPort`를 제거하는가?

`@LocalServerPort`는 `webEnvironment = RANDOM_PORT`에서 할당된 포트를 주입한다.
`webEnvironment = NONE`에서는 웹서버가 없으므로 포트가 할당되지 않아 에러가 발생한다.

#### `port()` vs `baseUri()`

| 메서드 | 동작 | 예시 |
|--------|------|------|
| `.port(8080)` | `http://localhost:8080`으로 요청 | 호스트의 로컬 서버 |
| `.baseUri("http://localhost:28080")` | 지정된 URL로 요청 | Docker 컨테이너의 서버 |

`baseUri()`를 사용하면 호스트명과 포트를 한 번에 지정할 수 있어,
Docker 컨테이너처럼 다른 호스트/포트를 사용하는 경우에 적합하다.

---

## 실행 흐름

```
./gradlew cucumberTest 실행

1. dockerUp (docker-compose up -d --wait)
   ├── PostgreSQL 컨테이너 시작
   ├── pg_isready → healthy
   ├── App 컨테이너 시작 (PostgreSQL 준비 후)
   ├── Spring Boot 기동, ddl-auto=create로 스키마 생성
   └── wget → healthy (앱 준비 완료)

2. cucumberTest (Cucumber 시나리오 실행)
   ├── Spring context 시작 (webEnvironment=NONE, 웹서버 미기동)
   ├── ddl-auto=validate → 스키마 검증 통과
   │
   ├── 시나리오 1: 카테고리 생성
   │   ├── @Before: DB 정리 (JDBC → localhost:5432)
   │   ├── When: POST /api/categories (HTTP → localhost:28080)
   │   └── Then: 응답 코드 200 검증
   │
   ├── 시나리오 2~6: 동일 패턴
   │
   └── Spring context 종료

3. dockerDown (docker-compose down)
   ├── App 컨테이너 정지 + 제거
   ├── PostgreSQL 컨테이너 정지 + 제거
   └── Docker 네트워크 제거
```

---

## 검증

```bash
# 1. Docker 이미지 빌드
./gradlew dockerBuild

# 2. 앱 + DB 시작 → Cucumber 테스트 → 종료 (한 줄)
./gradlew cucumberTest
# → 6개 시나리오 통과

# 3. H2 단위 테스트 (Docker 불필요, 영향 없음)
./gradlew test
# → 8개 테스트 통과

# 4. 수동 확인 (선택)
./gradlew dockerUp
curl http://localhost:28080/api/categories  # → []
./gradlew dockerDown
```

---

## 의사결정 기록

| 결정 | 이유 |
|------|------|
| Multi-stage Dockerfile | 빌드 도구(JDK, Gradle)를 최종 이미지에서 제외하여 경량화 |
| Builder는 jammy, Runtime은 alpine | Gradle 네이티브 라이브러리의 ARM64/musl 호환성 문제 회피 |
| `webEnvironment=NONE` | Docker 앱이 웹서버 역할, 테스트는 Repository 접근만 필요 |
| `ddl-auto` 역할 분담 (create/validate) | 스키마 생성 책임을 앱에, 검증 책임을 테스트에 분리 |
| 환경변수로 Spring 설정 주입 | main 코드 수정 없이 Docker 환경에 적응 |
| `cucumber.target.url` 커스텀 속성 | Docker 앱 URL을 설정 파일로 외부화, 환경별 유연한 변경 가능 |
| `runtimeOnly` PG 드라이버 | bootJar에 드라이버 포함 필요 (Docker 앱이 PostgreSQL 접속) |
| 포트 28080 사용 | 로컬 개발 서버(8080)와 충돌 방지 |
| `wget`으로 healthcheck | Alpine 이미지에 `curl` 미포함, `wget`은 기본 제공 |
| `start_period: 30s` | Spring Boot 기동 시간 고려, 불필요한 healthcheck 실패 방지 |
