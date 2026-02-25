# PostgreSQL + Docker Compose 학습 가이드

## 왜 H2 대신 PostgreSQL인가?

H2는 빠르지만 프로덕션 DB와 동작이 다르다.

- SQL 방언 차이 (예: `TRUNCATE` 문법, 타입 캐스팅)
- 트랜잭션/flush 타이밍 차이
- "내 컴에선 되는데" 문제

**Production Parity** — 테스트 DB를 프로덕션과 동일하게 맞추면 CI에서 깨지는 문제를 사전에 방지할 수 있다.

---

## 1. Docker Compose 기본 개념

### docker-compose.yml 구조

```yaml
services:
  postgres:           # 서비스 이름 (Docker 네트워크에서 hostname으로 사용)
    image: postgres:15
    environment:      # 컨테이너 환경 변수
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432:5432"   # Host:Container 포트 매핑
    healthcheck:      # 컨테이너 준비 상태 체크
      test: ["CMD-SHELL", "pg_isready -U test"]
      interval: 5s
      timeout: 3s
      retries: 10
```

### 핵심 키워드

| 키워드 | 설명 |
|--------|------|
| `services` | 실행할 컨테이너 목록 정의 |
| `image` | 사용할 Docker 이미지 |
| `environment` | 컨테이너 내부 환경 변수 (PostgreSQL 초기 DB/유저 설정) |
| `ports` | `Host포트:Container포트` 매핑. Host에서 `localhost:5432`로 접근 가능 |
| `healthcheck` | 컨테이너가 "준비 완료"인지 확인하는 체크 |

### Health Check가 필요한 이유

`docker-compose up -d` 후 컨테이너가 **started** 상태여도 PostgreSQL이 아직 연결을 받지 못할 수 있다.
`pg_isready`로 실제 연결 가능 여부를 확인해야 테스트가 안정적으로 실행된다.

### 주요 명령어

```bash
docker-compose up -d          # 백그라운드 실행
docker-compose down           # 종료 및 정리
docker-compose exec postgres pg_isready -U test  # DB 준비 확인
docker-compose logs postgres  # 로그 확인
docker ps                     # 실행 중인 컨테이너 목록
```

---

## 2. Spring Profile 분리

### 동작 원리

Spring Boot는 `spring.profiles.active` 값에 따라 설정 파일을 선택한다.

```
프로파일 없음  → application.properties (기본, H2 자동 설정)
cucumber      → application.properties + application-cucumber.properties (PostgreSQL)
```

`application-cucumber.properties`가 기본 설정을 **오버라이드**한다.

### application-cucumber.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/testdb
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

| 설정 | 설명 |
|------|------|
| `datasource.url` | `localhost:5432` — Host에서 Docker PostgreSQL로 접근 |
| `ddl-auto=create-drop` | 테스트 시작 시 스키마 생성, 종료 시 삭제 |
| `dialect` | Hibernate가 PostgreSQL 문법으로 SQL 생성 |

### 프로파일 활성화 방법

코드에 `@ActiveProfiles`를 넣지 않고 **Gradle task에서 시스템 프로퍼티로 주입**한다.

```groovy
// build.gradle
systemProperty 'spring.profiles.active', 'cucumber'
```

이렇게 하면 같은 테스트 코드가:
- `./gradlew test` → H2 (프로파일 없음)
- `./gradlew cucumberTest` → PostgreSQL (`cucumber` 프로파일)

---

## 3. DatabaseCleaner — H2/PostgreSQL 양쪽 호환

### 문제

H2와 PostgreSQL의 TRUNCATE 문법이 다르다.

| DB | FK 제약 해제 방법 |
|----|--------------------|
| H2 | `SET REFERENTIAL_INTEGRITY FALSE` → TRUNCATE → `SET REFERENTIAL_INTEGRITY TRUE` |
| PostgreSQL | `TRUNCATE TABLE t1, t2, t3 CASCADE` (한 문장으로 처리) |

### 해결: DataSource URL로 DB 종류 감지

```java
private boolean isPostgresql() {
    try (var connection = dataSource.getConnection()) {
        String url = connection.getMetaData().getURL();
        return url.contains("postgresql");
    } catch (Exception e) {
        return false;
    }
}
```

주의: `try-with-resources`로 커넥션을 반드시 닫아야 한다. 안 닫으면 매 시나리오마다 커넥션이 누수되어 풀이 고갈된다.

### 전체 코드

```java
public void clear() {
    if (isPostgresql()) {
        String tableList = String.join(", ", TABLES);
        jdbcTemplate.execute("TRUNCATE TABLE " + tableList + " CASCADE");
    } else {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
```

---

## 4. Gradle Task 자동화

### cucumberTest task 구조

```groovy
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    exclude 'gift/restassured/**'                           // Cucumber만 실행
    systemProperty 'spring.profiles.active', 'cucumber'     // PostgreSQL 프로파일

    doFirst {
        // 1. PostgreSQL 시작
        exec {
            commandLine 'docker-compose', 'up', '-d'
        }
        // 2. DB 준비 대기
        def maxRetries = 20
        for (int i = 0; i < maxRetries; i++) {
            try {
                exec {
                    commandLine 'docker-compose', 'exec', '-T', 'postgres', 'pg_isready', '-U', 'test'
                }
                break
            } catch (Exception e) {
                if (i == maxRetries - 1) throw e
                Thread.sleep(2000)
            }
        }
    }
    finalizedBy 'dockerDown'  // 3. 테스트 성공/실패 무관하게 DB 종료
}
```

### 핵심 개념

| 개념 | 설명 |
|------|------|
| `doFirst` | task 실행 직전에 호출되는 블록. Docker 시작 + healthcheck 대기 |
| `finalizedBy` | task 완료 후 항상 실행. 테스트 실패해도 DB가 정리됨 |
| `exec` | Gradle 내부에서 외부 명령 실행 |
| `systemProperty` | JVM 시스템 프로퍼티 설정 → Spring이 프로파일로 인식 |
| `-T` | `docker-compose exec`에서 TTY 할당 안 함 (CI 환경 호환) |

### 전체 task 분리 구조

```
./gradlew test             → Cucumber + H2 (빠른 피드백)
./gradlew restAssuredTest  → RestAssured + H2 (빠른 피드백)
./gradlew cucumberTest     → Cucumber + PostgreSQL (Production Parity)
```

---

## 5. 네트워크 구조

```
Host (개발 머신)
├── Gradle JVM
│   ├── Spring Boot (@SpringBootTest, 랜덤 포트)
│   ├── Cucumber 테스트 → localhost:랜덤포트 → Spring Boot
│   └── Spring Boot    → localhost:5432     → PostgreSQL
│
Docker
└── PostgreSQL 컨테이너 (5432:5432 포트 매핑)
```

- 테스트 코드와 Spring Boot는 **Host**에서 실행
- PostgreSQL만 **Docker 컨테이너**에서 실행
- Host에서 `localhost:5432`로 Docker의 PostgreSQL에 접근 (포트 매핑)

---

## 6. 트러블슈팅

### 커넥션 풀 고갈

**증상**: 10번째 시나리오부터 `HikariPool - Connection is not available, request timed out`

**원인**: `dataSource.getConnection()`을 호출하고 닫지 않으면 매번 커넥션이 누수됨

**해결**: `try-with-resources`로 반드시 커넥션 반환

```java
// BAD - 커넥션 누수
String url = dataSource.getConnection().getMetaData().getURL();

// GOOD - 자동 반환
try (var connection = dataSource.getConnection()) {
    String url = connection.getMetaData().getURL();
}
```

### 포트 충돌

**증상**: `Bind for 0.0.0.0:5432 failed: port is already allocated`

**해결**: `docker-compose down` 후 재시도, 또는 `docker ps`로 점유 컨테이너 확인

### docker compose vs docker-compose

| 명령 | 버전 |
|------|------|
| `docker compose` | Docker Compose V2 (Docker CLI 플러그인) |
| `docker-compose` | Docker Compose V1 (독립 실행 파일) |

환경에 맞는 명령어를 `build.gradle`에서 사용해야 한다.
