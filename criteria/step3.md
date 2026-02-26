# Step 3 - Docker E2E 테스트 환경 요구사항 충족 여부

## 요구사항 체크리스트

| # | 요구사항 | 충족 여부 |
|---|---------|----------|
| 1 | Dockerfile 작성 (Multi-stage build) | O |
| 2 | Docker Compose에 애플리케이션 서비스 추가 | O |
| 3 | 테스트가 Docker 컨테이너의 애플리케이션에 HTTP 요청 | O |
| 4 | 전체 시스템 빌드/시작/종료 자동화 | O |

## 검증 사항

| # | 검증 항목 | 결과 |
|---|----------|------|
| 1 | `./gradlew dockerBuild` | O - Multi-stage build로 이미지 생성 |
| 2 | `./gradlew dockerUp` | O - postgres + app 컨테이너 시작, healthy 확인 |
| 3 | `curl http://localhost:28080/api/categories` | O - 200 응답, `[]` 반환 |
| 4 | `./gradlew cucumberTest` | O - Docker 앱에 E2E 테스트 실행, BUILD SUCCESSFUL |
| 5 | `./gradlew dockerDown` | O - 전체 컨테이너 종료 및 정리 |

---

## 1. Dockerfile 작성 (Multi-stage build)

**충족 여부**: O

### 근거

#### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **Stage 1 (build)**: `eclipse-temurin:21-jdk`에서 `bootJar`로 애플리케이션을 빌드한다. 테스트는 제외한다(`-x test`).
- **Stage 2 (runtime)**: `eclipse-temurin:21-jre`에 JAR만 복사하여 경량 이미지를 생성한다.
- `curl`은 healthcheck용으로 설치한다.

---

## 2. Docker Compose에 애플리케이션 서비스 추가

**충족 여부**: O

### 근거

#### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:17
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    tmpfs:
      - /var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test -d gift_test"]
      interval: 3s
      timeout: 3s
      retries: 10

  app:
    build: .
    ports:
      - "28080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
      SPRING_DATASOURCE_USERNAME: test
      SPRING_DATASOURCE_PASSWORD: test
      SPRING_JPA_HIBERNATE_DDL_AUTO: create
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/categories"]
      interval: 5s
      timeout: 3s
      retries: 10
```

- `app` 서비스는 Dockerfile로 빌드한 이미지를 사용한다.
- `28080:8080` 포트 매핑으로 호스트에서 접근할 수 있다.
- 환경 변수로 PostgreSQL 접속 정보를 주입한다. Docker 내부 네트워크에서 `postgres:5432`로 접근한다.
- `depends_on` + `service_healthy`로 PostgreSQL이 준비된 후 앱이 시작된다.
- `pg_isready`와 `curl`을 이용한 healthcheck로 `--wait` 플래그가 정상 동작한다.

---

## 3. 테스트가 Docker 컨테이너의 애플리케이션에 HTTP 요청

**충족 여부**: O

### 근거

#### CucumberSpringConfiguration.java

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class Config {

        @Bean
        @Scope("cucumber-glue")
        public ApiClient apiClient(@Value("${test.server.port}") int port) {
            return new ApiClient(port);
        }
    }
}
```

- `WebEnvironment.NONE`: 테스트 JVM에서 웹 서버를 시작하지 않는다. 앱은 Docker 컨테이너에서 실행된다.
- `test.server.port=28080`: Docker 앱의 매핑된 포트로 HTTP 요청을 보낸다.
- Spring 컨텍스트는 로드되므로 JPA Repository를 통한 DB 직접 접근(데이터 초기화, 검증)은 유지된다.

#### application-test.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
test.server.port=28080
```

- 테스트 JVM은 `localhost:5432`로 PostgreSQL에 접속하고, `localhost:28080`으로 앱에 HTTP 요청한다.
- Docker 앱은 `postgres:5432`로 같은 PostgreSQL에 접속한다. 테스트 JVM과 앱이 동일한 DB를 공유한다.

---

## 4. 전체 시스템 빌드/시작/종료 자동화

**충족 여부**: O

### 근거

#### build.gradle

```gradle
tasks.register('startDb', Exec) {
    commandLine 'docker', 'compose', 'up', 'postgres', '-d', '--wait'
}

tasks.register('stopDb', Exec) {
    commandLine 'docker', 'compose', 'down'
}

tasks.register('dockerBuild', Exec) {
    commandLine 'docker', 'compose', 'build'
}

tasks.register('dockerUp', Exec) {
    dependsOn 'dockerBuild'
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('dockerDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}

tasks.named('test') {
    dependsOn 'startDb'
    finalizedBy 'stopDb'
    useJUnitPlatform()
    exclude '**/acceptance/**'
}

tasks.register('cucumberTest', Test) {
    dependsOn 'dockerUp'
    finalizedBy 'dockerDown'
    useJUnitPlatform {
        includeEngines 'junit-platform-suite'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    include '**/CucumberTest*'
}
```

#### 실행 흐름

```
./gradlew test          → startDb(postgres만) → 단위/통합 테스트 → stopDb
./gradlew cucumberTest  → dockerBuild → dockerUp(postgres+app) → E2E 테스트 → dockerDown
```

- `test` 태스크: PostgreSQL만 시작하고 단위/통합 테스트를 실행한다. `exclude '**/acceptance/**'`로 Cucumber 테스트를 제외한다.
- `cucumberTest` 태스크: Docker 이미지를 빌드하고, 전체 시스템(postgres + app)을 시작한 후 E2E 테스트를 실행하고, 종료한다.
- `startDb`는 `docker compose up postgres`로 postgres 서비스만 시작한다.
- `dockerUp`은 `dockerBuild`에 의존하여 항상 최신 이미지로 시작한다.
