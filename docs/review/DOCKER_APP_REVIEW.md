# Application 컨테이너화 코드 리뷰

## 총평

Application 컨테이너화가 전체적으로 잘 구현되어 있습니다. Multi-stage build, Docker 네트워크 아키텍처 이해, `webEnvironment = NONE` 전환, `ddl-auto` 역할 분리 등 핵심 개념을 정확히 적용했습니다. `./gradlew cucumberTest` 한 번으로 빌드 → 컨테이너 기동 → 테스트 → 정리까지 자동화되며, 8개 시나리오 모두 100% 통과합니다. 이전 리뷰 피드백(`finalizedBy` 활성화, `docker compose` 명령어)도 반영되었습니다.

---

## 요구사항 충족 체크리스트

| 요구사항 | 충족 여부 | 비고 |
|---|---|---|
| Dockerfile 작성 (Multi-stage build) | ✅ | Builder(JDK 25) + Runtime(JRE 25) 2단계 |
| Docker Compose에 애플리케이션 서비스 추가 | ✅ | `app` 서비스 + `depends_on` + healthcheck |
| 테스트가 Docker 컨테이너의 앱에 HTTP 요청 | ✅ | `webEnvironment = NONE` + `RestAssured.port = 28080` |
| 전체 시스템 빌드/시작/종료 자동화 | ✅ | `cucumberTest` → `dockerUp` → `dockerBuild` 체인 + `finalizedBy dockerDown` |
| `./gradlew dockerBuild` | ✅ | Docker 이미지 빌드 성공 |
| `./gradlew dockerUp` | ✅ | PostgreSQL + App 모두 healthy |
| `curl http://localhost:28080` | ✅ | 앱 응답 확인 가능 |
| `./gradlew cucumberTest` | ✅ | 8 tests, 0 failures, 100% |
| `./gradlew dockerDown` | ✅ | 컨테이너 + 네트워크 정리 완료 |
| README.md에 Docker 기반 실행 방법 추가 | ✅ | Docker 개별 명령어 및 표 업데이트 |
| 학습 내용 문서화 (선택) | 🔲 | `STEP2.md`에 실행 순서는 정리했으나 "배운 것 정리" 섹션은 비어 있음 |

---

## 검증 결과

```
./gradlew clean cucumberTest

✅ :dockerBuild     — Docker 이미지 빌드 (캐시 활용)
✅ :dockerUp        — postgres Healthy → app Healthy
✅ :cucumberTest    — 8/8 PASSED
✅ :dockerDown      — 컨테이너 + 네트워크 정리

./gradlew test

✅ AcceptanceTest   — 8/8 PASSED (H2, Docker 불필요)
```

Docker 이미지 크기: **~160MB** (`eclipse-temurin:25-jre` 기반)

---

## 발견된 문제점

### 🟡 경고: RestAssured 포트 28080이 하드코딩되어 있다

**파일:** `CucumberHooks.java:22-23`

```java
RestAssured.baseURI = "http://localhost";
RestAssured.port = 28080;
```

**파일:** `docker-compose.yml:19`

```yaml
ports:
  - "28080:8080"
```

**문제:** 포트 번호가 `docker-compose.yml`과 `CucumberHooks.java` 두 곳에 하드코딩되어 있습니다. 포트를 변경할 때 두 파일을 동시에 수정해야 하며, 한쪽만 변경하면 테스트가 실패합니다.

**개선 방향:** `application-cucumber.properties`에 테스트 대상 URL을 설정하고 `@Value`로 주입합니다.

```properties
# application-cucumber.properties
test.app.base-url=http://localhost:28080
```

```java
public class CucumberHooks {
    @Value("${test.app.base-url}")
    private String appBaseUrl;

    @Before
    public void setUp() throws SQLException {
        URI uri = URI.create(appBaseUrl);
        RestAssured.baseURI = uri.getScheme() + "://" + uri.getHost();
        RestAssured.port = uri.getPort();
        // ...
    }
}
```

이렇게 하면 포트 변경 시 properties 파일만 수정하면 됩니다. 다만 현재 구조에서도 기능적으로는 문제없으므로 심각도는 낮습니다.

---

### 🟡 경고: App healthcheck가 비즈니스 API를 사용한다

**파일:** `docker-compose.yml:31`

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/api/categories || exit 1"]
```

**문제:** `/api/categories`는 실제 DB를 조회하는 비즈니스 엔드포인트입니다. 이를 healthcheck로 사용하면:

1. 테이블 생성 전(`ddl-auto=create`가 완료되기 전) DB 에러로 healthcheck 실패 → 이건 `start_period: 15s`로 어느정도 완화됨
2. DB 커넥션 풀 고갈 시 앱은 살아 있는데 healthcheck 실패
3. healthcheck 요청이 `/api/categories`의 로그에 반복적으로 기록됨

**개선 방향:** Spring Boot Actuator를 추가하면 전용 health endpoint를 사용할 수 있습니다.

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
```

Actuator를 추가하지 않는다면, 현재 방식도 실용적으로 동작합니다. `start_period: 15s`로 초기 유예 시간을 준 것은 좋은 판단입니다.

---

### 🟡 경고: Runtime 이미지에 `apt-get install curl`이 포함되어 있다

**파일:** `Dockerfile:16`

```dockerfile
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
```

**문제:** healthcheck에서 `curl`을 사용하기 위해 설치한 것인데, 이는 런타임 이미지에 **불필요한 패키지와 공격 표면**을 추가합니다. 약 7MB 증가.

**대안 1:** `wget`은 debian 기반 이미지에 기본 설치되어 있는 경우가 많습니다.
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/api/categories || exit 1"]
```

**대안 2:** 다른 방식으로는 Java 프로세스 자체의 존재를 체크합니다.
```yaml
healthcheck:
  test: ["CMD-SHELL", "java -version || exit 1"]
```
(단, 이 방식은 앱이 HTTP 요청을 받을 수 있는지는 확인하지 않습니다)

**현실적 판단:** 테스트 환경 전용 이미지이므로 `curl` 설치는 허용 가능합니다. 프로덕션 이미지라면 제거를 권장합니다.

---

### 🟡 경고: Dockerfile이 Java 25 기반이지만, 힌트는 `eclipse-temurin:21-jre-alpine`이다

**파일:** `Dockerfile:1, 13`

```dockerfile
FROM eclipse-temurin:25-jdk AS builder
# ...
FROM eclipse-temurin:25-jre
```

**분석:** 프로젝트가 Java 25를 사용하므로 `eclipse-temurin:25-*`은 올바른 선택입니다. 힌트의 `21-jre-alpine`은 일반적인 예시일 뿐입니다.

다만 두 가지 참고 사항이 있습니다:

1. **alpine 이미지 미사용**: `eclipse-temurin:25-jre`는 Debian 기반(~160MB)이고, `eclipse-temurin:25-jre-alpine`이 있다면 ~80MB 이하로 줄일 수 있습니다. 다만 Java 25 + alpine 조합이 아직 지원되지 않을 수 있으므로 확인이 필요합니다.

2. **Java 25는 non-LTS**: Java 25는 단기 지원 릴리스입니다. 프로덕션에서는 Java 21(LTS)이나 Java 25 이후 LTS 버전을 사용하는 것이 일반적입니다. 학습 프로젝트에서는 문제없습니다.

---

### 🟢 잘한 점: Multi-stage build 레이어 캐싱 전략이 우수하다

**파일:** `Dockerfile:1-10`

```dockerfile
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src/main/ src/main/
RUN ./gradlew bootJar --no-daemon -x test
```

의존성 다운로드(`./gradlew dependencies`)와 소스 빌드(`./gradlew bootJar`)를 분리했습니다. 이 덕분에:

- `build.gradle`이 바뀌지 않으면 → 의존성 레이어 캐시 히트
- `src/main/` 코드만 바뀌면 → 의존성 다운로드 건너뛰고 JAR만 재빌드

Docker 빌드 시간을 크게 단축하는 핵심 패턴을 잘 적용했습니다.

---

### 🟢 잘한 점: `webEnvironment = NONE` 전환이 정확하다

**파일:** `CucumberSpringConfiguration.java:8`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

**왜 이것이 핵심인가:** 애플리케이션이 Docker 컨테이너에서 이미 실행 중이므로, 테스트 JVM에서 내장 Tomcat을 또 띄우면:

- 포트 충돌 가능
- 어느 서버로 요청을 보내는지 혼동
- 불필요한 리소스 낭비

`NONE`으로 설정하면 Spring 컨텍스트는 로드되지만(JdbcTemplate 등 Bean 사용 가능) 웹 서버는 시작하지 않습니다. 테스트는 Docker 앱 컨테이너(`localhost:28080`)에 HTTP 요청을 보내고, cleanup은 Host에서 DB(`localhost:5432`)에 직접 JDBC로 접근합니다.

이 아키텍처 분리가 이번 요구사항의 핵심이며, 정확히 구현되었습니다.

---

### 🟢 잘한 점: `ddl-auto` 역할 분리가 명확하다

| 위치 | 값 | 역할 |
|---|---|---|
| `docker-compose.yml` (env) | `SPRING_JPA_HIBERNATE_DDL_AUTO: create` | Docker 앱이 스키마를 생성 |
| `application-cucumber.properties` | `spring.jpa.hibernate.ddl-auto=none` | 테스트 JVM은 스키마에 손대지 않음 |

테스트 JVM에서 `ddl-auto=none`으로 설정한 이유를 STEP2.md에 명확히 기록한 것도 좋습니다: "스키마 생성은 Docker 앱이 담당".

---

### 🟢 잘한 점: `.dockerignore`가 적절하게 구성되어 있다

**파일:** `.dockerignore`

```
build/
.gradle/
.git/
.idea/
*.md
docs/
src/test/
```

빌드 캐시, IDE 설정, 테스트 코드, 문서를 모두 제외하여 빌드 컨텍스트를 최소화했습니다. 특히 `src/test/`를 제외한 것은 프로덕션 이미지에 테스트 코드가 포함되지 않아야 한다는 원칙에 부합합니다.

---

### 🟢 잘한 점: Docker 네트워크 이해가 정확하다

```
App (Container) → postgres:5432     (Docker 내부 네트워크, 서비스명 = hostname)
Test (Host)     → localhost:5432    (포트 매핑)
Test (Host)     → localhost:28080   (포트 매핑)
```

- App 컨테이너의 `SPRING_DATASOURCE_URL`은 `jdbc:postgresql://postgres:5432/gift_test` (Docker 네트워크의 서비스명)
- 테스트의 `application-cucumber.properties`는 `jdbc:postgresql://localhost:5432/gift_test` (Host에서 접근)

이 분리를 정확히 이해하고 구현했습니다.

---

### 🟢 잘한 점: 이전 리뷰 피드백 반영

| 피드백 | 반영 상태 |
|---|---|
| `finalizedBy 'dockerComposeDown'` 주석 해제 | ✅ `finalizedBy 'dockerDown'` 활성화 |
| `docker-compose` → `docker compose` | ✅ 모든 Gradle 태스크에서 `docker compose` 사용 |

---

## 구조 요약 (변경된 부분)

```
(root)
├── Dockerfile                                      ← NEW: Multi-stage build
├── .dockerignore                                   ← NEW: 빌드 컨텍스트 최적화
├── docker-compose.yml                              ← MOD: app 서비스 추가
├── build.gradle                                    ← MOD: dockerBuild/dockerUp/dockerDown 태스크
├── src/test/
│   ├── java/gift/cucumber/
│   │   ├── CucumberSpringConfiguration.java        ← MOD: webEnvironment = NONE
│   │   └── CucumberHooks.java                      ← MOD: @LocalServerPort 제거, 28080 고정
│   └── resources/
│       └── application-cucumber.properties          ← MOD: ddl-auto=none
└── README.md                                       ← MOD: Docker 실행 방법 추가
```

### 아키텍처 다이어그램

```
┌─ Host ─────────────────────────────────────────────────────────┐
│                                                                 │
│   ./gradlew cucumberTest                                        │
│       │                                                         │
│       ├─ JVM (Test)                                             │
│       │    ├─ Cucumber Steps ──HTTP──→ localhost:28080 ──┐      │
│       │    └─ JdbcTemplate   ──JDBC──→ localhost:5432 ───┤      │
│       │                                                   │      │
│  ┌─ Docker ──────────────────────────────────────────────┤      │
│  │                                                       │      │
│  │  ┌─ app (28080:8080) ─────────┐                      │      │
│  │  │  Spring Boot + JRE 25      │                      │      │
│  │  │  ──JDBC──→ postgres:5432 ──┤                      │      │
│  │  └────────────────────────────┘                      │      │
│  │                                                       │      │
│  │  ┌─ postgres (5432:5432) ─────┐                      │      │
│  │  │  PostgreSQL 17             │◄─────────────────────┘      │
│  │  └────────────────────────────┘                              │
│  │                                                               │
│  └───────────────────────────────────────────────────────────────│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 추가 조언

### 1. "왜 애플리케이션까지 컨테이너로 실행하는가?"

요구사항 2에서는 DB만 Docker로 실행했습니다. 이 구조에는 여전히 차이가 있습니다:

```
요구사항 2: Test JVM + 내장 Tomcat(Host) → PostgreSQL(Docker)
요구사항 3: Test JVM(Host) → App(Docker) → PostgreSQL(Docker)
프로덕션:   사용자 → App(서버/컨테이너) → DB(서버/컨테이너)
```

요구사항 3이 프로덕션에 훨씬 가깝습니다:
- 앱이 **컨테이너 네트워크** 안에서 DB에 접근 (localhost가 아닌 서비스명)
- JVM 버전, OS, 환경변수가 **Dockerfile에 의해 고정** (개발자 머신 환경에 의존하지 않음)
- 프로덕션과 동일한 **JAR 패키징 + JRE 런타임**에서 실행

이것이 진정한 **End-to-End** 테스트입니다.

### 2. Multi-stage build를 쓰는 이유

만약 single-stage로 빌드하면:

```dockerfile
# Single stage: ~600MB+
FROM eclipse-temurin:25-jdk
COPY . /app
RUN ./gradlew bootJar
ENTRYPOINT ["java", "-jar", "app.jar"]
# → JDK, Gradle, 소스코드, 빌드 캐시가 모두 최종 이미지에 포함
```

Multi-stage에서는:

```dockerfile
# Builder: 빌드만 하고 버림
FROM eclipse-temurin:25-jdk AS builder
# ... 빌드 ...

# Runtime: JRE + JAR만 (~160MB)
FROM eclipse-temurin:25-jre
COPY --from=builder /app/build/libs/*.jar app.jar
```

최종 이미지에 JDK, Gradle, 소스코드가 포함되지 않아:
- **이미지 크기 감소** (600MB+ → 160MB)
- **보안 강화** (소스코드, 빌드 도구 미포함)
- **배포 속도 향상** (작은 이미지 = 빠른 pull)

### 3. Docker Compose 네트워크의 서비스명 = hostname

`docker-compose.yml`에서 서비스 이름(`postgres`, `app`)은 Docker가 자동으로 DNS 이름으로 등록합니다. 같은 Compose 네트워크 안의 컨테이너들은 서비스명으로 서로를 찾을 수 있습니다:

```
app 컨테이너에서: ping postgres → 172.18.0.2 (Docker 내부 IP)
Host에서:         ping postgres → 찾을 수 없음 (Docker 네트워크 밖)
```

그래서 `SPRING_DATASOURCE_URL`이 `jdbc:postgresql://postgres:5432/gift_test`이고, 테스트의 properties는 `jdbc:postgresql://localhost:5432/gift_test`인 것입니다.

### 4. `settings.gradle` 누락 가능성 확인

`Dockerfile:6`에서 `settings.gradle`을 COPY하고 있습니다. `settings.gradle`이 프로젝트에 존재하는지, `.dockerignore`에 의해 제외되지 않는지 확인하세요. `*.md`는 제외하지만 `*.gradle`은 제외하지 않으므로 현재는 문제없습니다.

---

## 생각해보면 좋을 점

### 1. Docker 캐시 무효화와 빌드 시간

`COPY src/main/ src/main/`은 소스 코드 변경 시 이후 레이어가 모두 무효화됩니다. 현재 구조는 최적이지만, `build.gradle` 변경 시 의존성 다운로드부터 다시 시작됩니다. CI에서 빌드 캐시를 공유하려면 어떻게 해야 할까요? (힌트: `--cache-from`, BuildKit cache mounts)

### 2. 프로덕션 배포와의 차이

현재 Dockerfile은 **테스트 환경용**입니다. 프로덕션 배포 시 추가로 고려할 것:
- 비-root 사용자로 실행 (`RUN adduser --system appuser && USER appuser`)
- JVM 메모리 설정 (`JAVA_OPTS`, `-Xmx`)
- 시크릿 관리 (환경변수 대신 Docker Secrets / Vault)
- 로깅 설정 (stdout → 로그 수집 시스템)

### 3. 테스트 격리에 대한 심화 질문

현재 `TRUNCATE`로 데이터를 초기화하지만, 테스트 JVM에서 JDBC로 Docker DB에 직접 접근합니다. 만약 앱 컨테이너가 트랜잭션 중일 때 `TRUNCATE`가 실행되면 어떤 일이 벌어질까요? (힌트: Cucumber의 `@Before`는 시나리오 시작 전에 실행되므로 앱이 idle 상태일 때 cleanup이 수행됩니다. 하지만 비동기 처리가 있다면?)

### 4. `docker compose build` vs `docker buildx`

`dockerBuild` 태스크가 매번 실행됩니다(`cucumberTest` → `dockerUp` → `dockerBuild`). 소스가 변경되지 않아도 빌드 컨텍스트를 전송하고 캐시를 확인하는 오버헤드가 있습니다. Gradle의 `inputs`/`outputs`를 활용하여 소스 변경 시에만 빌드하는 방법도 있습니다.

### 5. 포트 충돌 대응

`5432`와 `28080`이 고정되어 있으므로, 로컬에 PostgreSQL이 이미 실행 중이거나 다른 프로젝트가 같은 포트를 사용하면 충돌합니다. 이를 해결하는 방법:
- 환경변수로 포트를 주입: `ports: - "${APP_PORT:-28080}:8080"`
- 랜덤 포트 + `docker compose port` 명령으로 할당된 포트 조회

---

**최종 평가: 핵심 요구사항을 모두 충족했으며, 아키텍처 전환(내장 서버 → Docker 컨테이너)의 개념을 정확히 이해하고 구현했습니다.** Multi-stage build 캐싱 전략, `webEnvironment = NONE` 전환, Docker 네트워크 분리, Gradle 태스크 체인이 모두 올바릅니다. 포트 하드코딩과 healthcheck 엔드포인트 개선만 반영하면 더 견고한 구조가 됩니다.
