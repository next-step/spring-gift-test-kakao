---
name: dockerize-app
description: Spring Boot 애플리케이션을 Docker로 컨테이너화한다. Dockerfile 작성, docker-compose.yml에 앱 서비스 추가, Gradle 태스크 등록 등 Docker 기반 배포 환경을 구성한다.
disable-model-invocation: true
argument-hint: (인자 없음)
---

Spring Boot 애플리케이션을 컨테이너화한다. 아래 단계를 순서대로 수행한다.

## 전제 조건

- 요구사항 2(PostgreSQL + Docker Compose)가 완료된 상태여야 한다
- docker-compose.yml에 PostgreSQL 서비스가 구성되어 있어야 한다

## 1단계: .dockerignore 작성

프로젝트 루트에 `.dockerignore`를 생성하여 불필요한 파일을 제외한다.

```
.git
.gradle
.claude
.idea
build
*.md
```

## 2단계: Dockerfile 작성 (Multi-stage build)

프로젝트 루트에 `Dockerfile`을 생성한다.

```dockerfile
# Builder stage: Gradle로 빌드
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage: 경량 JRE로 실행
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Multi-stage build를 사용하는 이유:
- Builder stage에는 JDK, Gradle, 소스코드가 포함되지만 최종 이미지에는 JRE + JAR만 포함
- 이미지 크기를 최소화하고 보안 표면을 줄인다

## 3단계: docker-compose.yml에 앱 서비스 추가

기존 docker-compose.yml에 애플리케이션 서비스를 추가한다.

```yaml
services:
  postgres:
    # ... 기존 설정 유지

  app:
    build: .
    ports:
      - "28080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
      SPRING_DATASOURCE_USERNAME: test
      SPRING_DATASOURCE_PASSWORD: test
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/actuator/health || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 30s
```

네트워크 구조:
- 컨테이너 내부: `app` → `postgres:5432` (Docker service name이 hostname)
- 호스트(테스트): `localhost:28080` → `app`, `localhost:5432` → `postgres`

## 4단계: Gradle 태스크 업데이트

```groovy
tasks.register('dockerBuild', Exec) {
    commandLine 'docker', 'compose', 'build'
}

tasks.register('dockerUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('dockerDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}
```

## 5단계: 테스트 설정 변경

Docker 컨테이너의 앱에 HTTP 요청을 보내므로 embedded 서버가 필요 없다.

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
    }
}
```

- `webEnvironment = NONE`: 테스트 프로세스 내에서 Spring Boot 서버를 띄우지 않는다
- 테스트는 호스트에서 실행되고, Docker 컨테이너의 앱(28080)에 HTTP 요청을 보낸다

## 6단계: DB 직접 접근 (cleanup 용도)

테스트에서 DB 초기화를 위해 JdbcTemplate으로 PostgreSQL에 직접 접근한다.

application-cucumber.properties에서 DB 연결 설정을 유지한다:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
```

- 테스트(호스트) → `localhost:5432` → PostgreSQL 컨테이너
- 앱(컨테이너) → `postgres:5432` → PostgreSQL 컨테이너

같은 DB를 다른 hostname으로 접근하는 구조이다.

## 7단계: Spring Boot Actuator 추가 (healthcheck 용)

healthcheck에 actuator health endpoint를 사용한다면 의존성을 추가한다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

또는 healthcheck를 curl/wget 대신 TCP 체크로 대체할 수도 있다.

## 8단계: 검증

```bash
./gradlew dockerBuild
./gradlew dockerUp
curl http://localhost:28080    # 앱 응답 확인
./gradlew cucumberTest         # Docker 환경에서 테스트
./gradlew dockerDown
```

- `docker ps`로 컨테이너 상태를 확인한다
- `docker logs <container>`로 앱/DB 로그를 확인한다

## 트러블슈팅

| 문제 | 해결 |
|------|------|
| 앱이 DB 연결 실패 | `depends_on: condition: service_healthy` 확인 |
| 포트 충돌 | `docker ps`로 기존 컨테이너 확인, `docker compose down` 후 재시작 |
| 이미지 캐시 문제 | `docker compose build --no-cache` |
| 앱 시작 지연 | healthcheck의 `start_period` 조정 |
