# Application 컨테이너화 학습 기록

## 1. 왜 애플리케이션까지 컨테이너로 실행하는가?

### Production Parity 완성

요구사항 2에서는 DB만 Docker로 실행하고, 앱은 호스트에서 직접 실행했다.
하지만 프로덕션에서는 앱도 컨테이너로 실행되므로, 테스트 환경도 동일하게 구성해야
실제 배포 시 발생할 수 있는 문제를 미리 잡을 수 있다.

| 항목 | 요구사항 2 | 요구사항 3 |
|---|---|---|
| DB | Docker (PostgreSQL) | Docker (PostgreSQL) |
| App | 호스트 (내장 톰캣) | Docker (컨테이너) |
| 네트워크 | 호스트 내부 | Docker 네트워크 |
| Production Parity | 부분적 | 완전 |

컨테이너화를 통해 발견할 수 있는 문제 예시:
- 환경변수 누락 (호스트에서는 되지만 컨테이너에서는 안 되는 설정)
- 네트워크 설정 차이 (`localhost` vs 서비스 이름)
- 파일 시스템 차이 (경로, 권한)
- JVM 버전/옵션 차이

---

## 2. Multi-stage Build

### 2.1 Multi-stage Build란?

하나의 Dockerfile 안에서 여러 단계(stage)를 정의하여,
**빌드에 필요한 도구**와 **실행에 필요한 최소 환경**을 분리하는 기법이다.

```dockerfile
# Stage 1: Build (JDK + Gradle로 빌드)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime (JRE만으로 실행)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2.2 Builder Stage와 Runtime Stage의 역할

| Stage | 베이스 이미지 | 역할 | 포함 내용 |
|---|---|---|---|
| builder | `eclipse-temurin:21-jdk` | 소스 코드 빌드 | JDK, Gradle, 소스코드, 의존성 |
| runtime | `eclipse-temurin:21-jre-alpine` | JAR 실행 | JRE, app.jar만 |

### 2.3 왜 Multi-stage를 사용하는가?

**단일 stage로 빌드하면:**
- 최종 이미지에 JDK, Gradle, 소스코드가 모두 포함됨
- 이미지 크기: ~500MB 이상
- 보안 위험: 소스코드가 이미지에 노출

**Multi-stage로 빌드하면:**
- 최종 이미지에 JRE + JAR만 포함
- 이미지 크기: ~100MB
- 빌드 도구/소스코드가 최종 이미지에 없음

`COPY --from=builder`가 핵심: 빌드 결과물(JAR)만 builder stage에서 가져온다.

### 2.4 Dockerfile 각 명령어 설명

```dockerfile
FROM eclipse-temurin:21-jdk AS builder   # JDK 포함 이미지, 'builder'로 이름 지정
WORKDIR /app                             # 작업 디렉토리 설정
COPY . .                                 # 소스코드 전체 복사
RUN chmod +x gradlew && \               # Gradle Wrapper 실행 권한 부여
    ./gradlew bootJar --no-daemon -x test  # JAR 빌드 (테스트 제외, 데몬 없이)

FROM eclipse-temurin:21-jre-alpine       # 경량 JRE 이미지 (새 stage 시작)
WORKDIR /app                             # 작업 디렉토리 설정
COPY --from=builder /app/build/libs/*.jar app.jar  # builder에서 JAR만 복사
EXPOSE 8080                              # 문서화 목적 (실제 포트 매핑은 docker-compose에서)
ENTRYPOINT ["java", "-jar", "app.jar"]   # 컨테이너 시작 시 실행할 명령
```

### 2.5 트러블슈팅: Alpine + Gradle 호환 문제

**발생한 문제:** Builder stage에서 `eclipse-temurin:21-jdk-alpine` 사용 시 SIGSEGV 크래시

**원인:** Gradle 8.4의 네이티브 라이브러리(`libnative-platform-file-events.so`)가
Alpine Linux(musl libc) + aarch64(Apple Silicon)에서 호환되지 않음

**해결:** Builder를 `eclipse-temurin:21-jdk` (Debian 기반)로 변경.
Runtime은 Alpine 유지 (JAR 실행에는 네이티브 라이브러리 불필요)

**교훈:** Alpine은 가볍지만, 네이티브 라이브러리를 사용하는 도구에서 호환 문제가 발생할 수 있다.
빌드 도구는 Debian 기반, 런타임은 Alpine 기반이 안전한 조합이다.

---

## 3. .dockerignore

```
.gradle
build
.git
*.md
docker-compose.yml
Dockerfile
```

**왜 필요한가?**

`docker build` 실행 시 현재 디렉토리 전체가 "빌드 컨텍스트"로 Docker 데몬에 전송된다.
불필요한 파일을 제외하면:
- 빌드 컨텍스트 전송 시간 단축
- Docker 캐시 무효화 방지 (`.git` 변경 때마다 캐시 깨지는 문제)
- 이미지에 불필요한 파일 포함 방지

---

## 4. Docker Compose 서비스 구성

### 4.1 app 서비스 정의

```yaml
app:
  build: .
  ports:
    - "28080:8080"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
    SPRING_DATASOURCE_USERNAME: test
    SPRING_DATASOURCE_PASSWORD: test
    SPRING_JPA_HIBERNATE_DDL_AUTO: update
  depends_on:
    postgres:
      condition: service_healthy
  healthcheck:
    test: ["CMD-SHELL", "wget --spider -q http://localhost:8080/actuator/health || exit 1"]
    interval: 5s
    timeout: 3s
    retries: 10
```

### 4.2 각 설정 항목 설명

| 항목 | 값 | 설명 |
|---|---|---|
| `build: .` | 현재 디렉토리 | Dockerfile로 이미지 빌드 |
| `ports: "28080:8080"` | 호스트:컨테이너 | 호스트 28080 → 컨테이너 8080 |
| `SPRING_DATASOURCE_URL` | `postgres:5432` | Docker 내부 네트워크에서 service name으로 접근 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | 앱이 테이블 생성/관리 |
| `depends_on: condition` | `service_healthy` | PostgreSQL ready 후 앱 시작 |
| healthcheck | `wget` + Actuator `/actuator/health` | 업계 표준 헬스체크 엔드포인트 사용. Alpine에 curl 없으므로 wget 사용 |
| `retries: 10` | 10회 | 앱 시작이 DB보다 오래 걸림 |

### 4.3 depends_on의 condition

```yaml
depends_on:
  postgres:
    condition: service_healthy   # healthcheck 통과 후 시작
```

`depends_on`만 쓰면 컨테이너 **시작 순서**만 보장한다 (healthy 여부 무관).
`condition: service_healthy`를 추가해야 PostgreSQL이 **실제로 쿼리를 받을 수 있는 상태**가 된 후에 앱이 시작된다.

---

## 5. Docker 네트워크 이해

### 5.1 service name이 hostname이 되는 원리

Docker Compose는 자동으로 네트워크를 생성하고, 모든 서비스를 이 네트워크에 연결한다.
각 서비스의 이름이 DNS hostname으로 등록된다.

```
Docker Network: spring-gift-test-kakao_default
  ├── postgres (hostname: postgres, 내부 IP: 172.18.0.2)
  └── app      (hostname: app,      내부 IP: 172.18.0.3)
```

따라서 앱 컨테이너에서 `postgres:5432`로 접근하면 PostgreSQL 컨테이너로 연결된다.

### 5.2 컨테이너 내부 vs 호스트 접근 경로

| 접근 주체 | PostgreSQL 접근 | App 접근 |
|----------|----------------|---------|
| 앱 (컨테이너) | `postgres:5432` | — |
| 테스트 (호스트) | `localhost:25432` | `localhost:28080` |

```
┌──── Docker Network ────────────────────┐
│                                         │
│  postgres:5432 ←──── app:8080           │
│      │                    │             │
└──────┼────────────────────┼─────────────┘
       │ port mapping       │ port mapping
       ▼                    ▼
  localhost:25432      localhost:28080
       │                    │
  ┌────┴────────────────────┴────┐
  │     Test Code (Host)          │
  │  JDBC (cleanup)  HTTP (API)   │
  └───────────────────────────────┘
```

---

## 6. webEnvironment = NONE

### 6.1 왜 NONE으로 변경하는가?

요구사항 2에서는 테스트가 직접 Spring Boot를 기동하고 (`RANDOM_PORT`),
내장 톰캣으로 HTTP 요청을 처리했다.

요구사항 3에서는 **Docker 컨테이너가 앱을 실행**하므로,
테스트에서 또 웹 서버를 기동하면 불필요한 중복이 된다.

| webEnvironment | 동작 | 용도 |
|---|---|---|
| `RANDOM_PORT` | Spring Boot + 내장 톰캣 기동 | 테스트가 직접 앱 실행 |
| `NONE` | Spring 컨텍스트만 로드 (웹 서버 없음) | 외부 앱에 요청 |

### 6.2 NONE에서 사용 가능한 것 / 불가능한 것

| 기능 | 사용 가능? | 이유 |
|---|---|---|
| `@Autowired JdbcTemplate` | O | DataSource는 웹과 무관 |
| `@Autowired OptionRepository` | O | JPA는 웹과 무관 |
| `@LocalServerPort` | X | 웹 서버가 없으므로 포트 없음 |
| `RestAssured.port = port` | X → 수동 설정 | 포트를 직접 28080으로 지정 |

### 6.3 RestAssured 설정 변경

```java
// Before (요구사항 2)
@LocalServerPort
int port;

@Before
public void setUp() {
    RestAssured.port = port;  // Spring Boot가 기동한 랜덤 포트
}

// After (요구사항 3)
@Before
public void setUp() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 28080;  // Docker 앱의 매핑 포트
}
```

---

## 7. ddl-auto 충돌 방지

### 7.1 문제 상황

Docker 앱과 테스트 Spring 컨텍스트가 **같은 DB에 동시 접속**한다.
둘 다 `create-drop`을 사용하면:

1. Docker 앱 시작 → 테이블 생성
2. 테스트 컨텍스트 시작 → 테이블 DROP → 재생성
3. Docker 앱의 JPA EntityManager가 스키마 변경을 모름 → 오류

### 7.2 해결: 역할 분리

| JVM | ddl-auto | 역할 |
|-----|----------|------|
| Docker 앱 | `update` | 테이블 생성/변경 담당 |
| 테스트 컨텍스트 | `validate` | 테이블 존재 확인만 |

```properties
# Docker 앱 (docker-compose.yml 환경변수)
SPRING_JPA_HIBERNATE_DDL_AUTO: update

# 테스트 (application-cucumber.properties)
spring.jpa.hibernate.ddl-auto=validate
```

`validate`는 엔티티와 테이블 구조가 일치하는지 확인만 하고, 스키마를 변경하지 않는다.
테이블이 없으면 즉시 실패하므로, Docker 앱이 정상 시작되지 않은 경우 빠르게 감지할 수 있다.

---

## 8. Gradle 태스크 구조

### 8.1 태스크 목록

```gradle
tasks.register('dockerBuild', Exec) {
    commandLine 'docker', 'compose', 'build'
}

tasks.register('dockerUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('dockerDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}

cucumberTest.dependsOn(dockerUp)
cucumberTest.finalizedBy(dockerDown)
```

### 8.2 자동 실행 흐름

```
./gradlew cucumberTest
  │
  ├── dockerUp        (dependsOn: 테스트 전 자동 실행)
  │   ├── postgres 시작 → Healthy
  │   └── app 빌드 + 시작 → Healthy
  │
  ├── cucumberTest    (7개 시나리오 실행)
  │
  └── dockerDown      (finalizedBy: 성공/실패 관계없이 실행)
      └── 모든 컨테이너 + 네트워크 정리
```

### 8.3 수동 실행 (개별 태스크)

```bash
./gradlew dockerBuild    # 이미지만 빌드 (코드 변경 후)
./gradlew dockerUp       # 시스템 시작 (디버깅, 수동 테스트)
./gradlew dockerDown     # 시스템 종료
```

---

## 9. 주요 의사결정과 이유

| 결정 | 이유 |
|---|---|
| Builder: Debian, Runtime: Alpine | Gradle 네이티브 라이브러리가 Alpine에서 크래시. JAR 실행은 Alpine에서 문제 없음 |
| 포트 28080 | 호스트의 8080과 충돌 방지. 테스트 전용 포트임을 명시 |
| Spring Actuator healthcheck | 업계 표준 `/actuator/health` 엔드포인트 사용. 비즈니스 API 의존 제거 |
| `ddl-auto=update` (앱) | 앱이 스키마 관리 담당. `create`는 매번 DROP하므로 위험 |
| `ddl-auto=validate` (테스트) | 스키마 변경하지 않고 확인만. 앱과 충돌 방지 |
| `depends_on: service_healthy` | PostgreSQL ready 후 앱 시작. 시작 순서만으로는 불충분 |

---

## 10. 탐구 질문과 답변

**Q. Multi-stage build는 무엇이고 왜 사용하는가?**

하나의 Dockerfile에서 빌드와 실행을 분리하는 기법이다.
Builder stage에서 JDK + Gradle로 JAR를 만들고,
Runtime stage에서 JRE만으로 JAR를 실행한다.
최종 이미지에 소스코드/빌드도구가 포함되지 않아 가볍고 안전하다.

**Q. Docker 네트워크에서 service name이 어떻게 hostname이 되는가?**

Docker Compose가 자동으로 생성하는 네트워크에 내장 DNS 서버가 있다.
각 서비스 이름을 DNS 레코드로 등록하므로,
`postgres`라는 이름으로 PostgreSQL 컨테이너의 IP를 resolve할 수 있다.

**Q. 컨테이너 내부에서는 postgres:5432, 테스트에서는 localhost:25432인 이유는?**

컨테이너끼리는 Docker 내부 네트워크를 통해 service name으로 통신한다.
호스트에서는 port mapping(`25432:5432`, `28080:8080`)을 통해 접근한다.
같은 서비스지만 접근 경로가 다른 이유는 네트워크 경계가 다르기 때문이다.

**Q. webEnvironment = NONE을 사용하는 이유는?**

Docker 컨테이너가 앱을 실행하므로 테스트에서 또 웹 서버를 기동할 필요가 없다.
`NONE`으로 설정하면 Spring 컨텍스트(DataSource, JPA)만 로드하고
웹 서버는 기동하지 않는다. HTTP 요청은 RestAssured가 Docker 앱으로 직접 보낸다.

**Q. 왜 JdbcTemplate은 필요한가? (cleanup 용도)**

각 시나리오 실행 전 DB를 초기화해야 테스트 격리가 보장된다.
Docker 앱에는 "모든 테이블 TRUNCATE" API가 없으므로,
테스트가 직접 DB에 JDBC로 접속하여 TRUNCATE를 실행한다.

```java
// 테스트 (호스트) → JDBC → localhost:25432 → PostgreSQL (Docker)
jdbcTemplate.execute("TRUNCATE TABLE option, product, category, wish, member RESTART IDENTITY CASCADE");
```

**Q. .dockerignore는 왜 필요한가?**

`docker build` 시 현재 디렉토리 전체가 Docker 데몬에 전송된다.
`.git`(수백 MB), `build/`(빌드 결과물), `.gradle/`(캐시) 등을 제외하면
전송 시간이 단축되고, 불필요한 캐시 무효화를 방지할 수 있다.

---

## 11. 트러블슈팅 키워드

```bash
docker ps                    # 실행 중인 컨테이너 상태 확인
docker logs <컨테이너이름>     # 컨테이너 로그 확인
docker exec -it <컨테이너> sh  # 컨테이너 내부 접속
docker system prune           # 사용하지 않는 이미지/컨테이너 정리
docker compose logs app       # app 서비스 로그만 확인
docker compose ps             # Compose 서비스 상태 확인
```
