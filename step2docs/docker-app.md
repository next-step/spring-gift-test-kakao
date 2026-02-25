# Application 컨테이너화 학습 가이드

## 왜 앱까지 컨테이너화하는가?

요구사항 2에서 DB만 Docker로 전환했지만, 앱은 여전히 Host에서 `@SpringBootTest`로 실행했다.
이 경우 Host의 JDK 버전, OS, 환경 설정에 따라 동작이 달라질 수 있다.

**앱까지 컨테이너화하면:**
- 프로덕션과 완전히 동일한 환경에서 E2E 테스트
- "내 컴에선 되는데" 문제 완전 해결
- CI/CD에서도 동일한 방식으로 실행 가능

---

## 1. Multi-stage Build

### 왜 Multi-stage인가?

단일 stage로 빌드하면 JDK, Gradle, 소스코드가 모두 최종 이미지에 포함된다.
Multi-stage build는 빌드 환경과 실행 환경을 분리하여 이미지를 경량화한다.

### Dockerfile

```dockerfile
# Builder stage — JDK로 빌드
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

# Runtime stage — JRE로 실행
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Stage | 베이스 이미지 | 역할 | 최종 이미지에 포함? |
|-------|-------------|------|-------------------|
| Builder | `eclipse-temurin:21-jdk` | Gradle 빌드, bootJar 생성 | X |
| Runtime | `eclipse-temurin:21-jre-alpine` | jar 실행만 | O |

### 핵심 명령어

| 명령 | 설명 |
|------|------|
| `FROM ... AS builder` | 빌드 stage에 이름 부여 |
| `COPY --from=builder` | builder stage의 결과물만 복사 |
| `eclipse-temurin:21-jre-alpine` | JRE만 포함된 경량 Alpine 이미지 |
| `ENTRYPOINT` | 컨테이너 시작 시 실행할 명령 |

---

## 2. .dockerignore

```
.gradle
build
.git
.claude
step2docs
*.md
```

Docker 빌드 시 `COPY . .`가 모든 파일을 빌드 컨텍스트로 전송한다.
`.dockerignore`로 불필요한 파일을 제외하면:
- 빌드 컨텍스트 전송 시간 단축
- 캐시 무효화 방지 (`.git` 변경으로 인한 불필요한 재빌드 방지)
- 이미지 크기 감소

---

## 3. Docker Compose — App 서비스 추가

```yaml
services:
  postgres:
    image: postgres:15
    # ... (기존 설정)

  app:
    build: .                          # 현재 디렉토리의 Dockerfile로 빌드
    ports:
      - "28080:8080"                  # Host:Container 포트 매핑
    depends_on:
      postgres:
        condition: service_healthy    # PostgreSQL 준비 후 시작
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/testdb
      SPRING_DATASOURCE_USERNAME: test
      SPRING_DATASOURCE_PASSWORD: test
      SPRING_JPA_HIBERNATE_DDL_AUTO: create
      SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT: org.hibernate.dialect.PostgreSQLDialect
```

### 핵심 개념

**`depends_on: condition: service_healthy`**

`depends_on`만 쓰면 PostgreSQL 컨테이너가 "시작"된 후 앱이 시작되지만,
PostgreSQL이 실제로 연결을 받을 준비가 안 된 상태일 수 있다.
`condition: service_healthy`는 healthcheck가 통과한 후에만 앱을 시작한다.

**`build: .`**

`image: xxx` 대신 `build: .`를 쓰면 Docker Compose가 직접 이미지를 빌드한다.
`docker-compose up --build`로 코드 변경 후 재빌드할 수 있다.

**포트 매핑 `28080:8080`**

- 컨테이너 내부: Spring Boot가 8080에서 실행
- Host: 28080으로 접근 (기본 8080과 충돌 방지)

**환경변수로 Spring 설정 주입**

Spring Boot는 `SPRING_DATASOURCE_URL` 환경변수를 자동으로 `spring.datasource.url`로 매핑한다.
Docker Compose의 `environment`로 컨테이너에 주입하면 `application.properties`를 수정하지 않아도 된다.

---

## 4. Docker 네트워크

### service name = hostname

Docker Compose는 같은 `docker-compose.yml`의 서비스들을 하나의 네트워크에 넣는다.
서비스 이름이 곧 DNS hostname이 된다.

```
App 컨테이너에서:
  postgres:5432  →  PostgreSQL 컨테이너로 연결됨
  localhost:5432 →  연결 실패 (자기 자신에게 PostgreSQL 없음)
```

### 전체 네트워크 구조

```
Host (개발 머신)
├── Gradle JVM (테스트)
│   ├── Cucumber 테스트  → localhost:28080 → Docker App (HTTP)
│   ├── Repository       → localhost:5432  → Docker PostgreSQL (JDBC)
│   └── DatabaseCleaner  → localhost:5432  → Docker PostgreSQL (JDBC)
│
Docker Network
├── App (28080:8080)     → postgres:5432   → PostgreSQL (JDBC)
└── PostgreSQL (5432:5432)
```

### 왜 주소가 다른가?

| 누가 | 어디로 | 왜 |
|------|--------|-----|
| 테스트 (Host) → App | `localhost:28080` | Host에서 Docker 포트 매핑으로 접근 |
| 테스트 (Host) → DB | `localhost:5432` | Host에서 Docker 포트 매핑으로 접근 |
| App (Container) → DB | `postgres:5432` | Docker 네트워크 내부, service name이 hostname |

---

## 5. webEnvironment = NONE

### 변경 전 (요구사항 2)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```
- Spring Boot가 embedded 서버를 시작
- 테스트가 embedded 서버에 HTTP 요청

### 변경 후 (요구사항 3)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```
- embedded 서버를 시작하지 않음
- 앱은 Docker 컨테이너에서 실행
- 테스트는 `localhost:28080`으로 Docker 앱에 HTTP 요청

### 왜 Spring Context는 여전히 필요한가?

`webEnvironment = NONE`이지만 `@SpringBootTest`는 유지한다.
테스트 JVM에서 아직 필요한 것들:
- **Repository** — 테스트 데이터 삽입 (`Fixtures`로 DB에 직접 저장)
- **DatabaseCleaner** — 시나리오마다 DB 초기화 (`JdbcTemplate`으로 TRUNCATE)

둘 다 `localhost:5432`로 Docker PostgreSQL에 JDBC 연결한다.

### CucumberHooks 변경

```java
// 변경 전: embedded 서버 포트
@LocalServerPort
int port;

@Before
public void setUp() {
    RestAssured.port = port;       // 랜덤 포트
}

// 변경 후: Docker 앱 포트 고정
@Before
public void setUp() {
    RestAssured.port = 28080;      // Docker 앱 포트
}
```

---

## 6. Gradle Task 자동화

### 전체 task 구조

```groovy
// 이미지 빌드
tasks.register('dockerBuild', Exec) {
    commandLine 'docker-compose', 'build'
}

// 전체 기동
tasks.register('dockerUp', Exec) {
    commandLine 'docker-compose', 'up', '-d'
}

// 전체 종료
tasks.register('dockerDown', Exec) {
    commandLine 'docker-compose', 'down'
}

// 테스트 (빌드 + 기동 + 테스트 + 종료)
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    exclude 'gift/restassured/**'
    systemProperty 'spring.profiles.active', 'cucumber'
    doFirst {
        exec { commandLine 'docker-compose', 'up', '-d', '--build' }
        // App healthcheck 대기
    }
    finalizedBy 'dockerDown'
}
```

### `--build` 플래그

`docker-compose up -d --build`는 코드가 변경되었을 때 이미지를 재빌드한 후 시작한다.
`cucumberTest`에서 사용하면 항상 최신 코드로 테스트할 수 있다.

### App healthcheck 대기

PostgreSQL은 `depends_on: service_healthy`로 준비를 보장하지만,
Spring Boot 앱은 시작에 시간이 걸린다. `curl`로 응답이 올 때까지 대기한다.

```groovy
def maxRetries = 30
for (int i = 0; i < maxRetries; i++) {
    try {
        exec { commandLine 'curl', '-s', '-o', '/dev/null', 'http://localhost:28080' }
        break
    } catch (Exception e) {
        if (i == maxRetries - 1) throw e
        Thread.sleep(3000)
    }
}
```

- `-s`: 진행 상황 출력 안 함
- `-o /dev/null`: 응답 본문 무시
- HTTP 상태 코드와 무관하게 연결만 되면 성공 (루트 경로에 매핑이 없어 404지만 서버는 동작 중)

---

## 7. 트러블슈팅

### 주요 명령어

| 명령 | 용도 |
|------|------|
| `docker ps` | 실행 중인 컨테이너 목록 확인 |
| `docker logs <container>` | 컨테이너 로그 확인 (앱 시작 실패 원인 파악) |
| `docker exec -it <container> bash` | 컨테이너 내부 진입 |
| `docker system prune` | 미사용 이미지/컨테이너/네트워크 정리 |

### curl healthcheck가 실패하는 경우

**증상**: `Process 'command 'curl'' finished with non-zero exit value 22`

**원인**: `curl -sf`의 `-f` 플래그가 HTTP 4xx/5xx를 에러로 처리

**해결**: `-f` 제거, `-o /dev/null`만 사용 — 연결 가능 여부만 확인

### 앱이 PostgreSQL에 연결 실패

**증상**: `Connection refused` in Docker app logs

**확인**: `docker logs spring-gift-test-kakao-app-1`

**원인**: `depends_on`에 `condition: service_healthy`가 없으면 DB 준비 전 앱이 시작됨

### 포트 충돌

**증상**: `Bind for 0.0.0.0:28080 failed: port is already allocated`

**해결**: `docker-compose down` 후 재시도, 또는 `docker ps`로 점유 컨테이너 확인
