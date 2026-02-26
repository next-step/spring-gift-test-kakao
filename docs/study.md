# 학습 정리

## 1단계: Cucumber BDD

### Gherkin과 Given/When/Then

Cucumber는 `.feature` 파일에 Gherkin 문법으로 시나리오를 작성한다.

```gherkin
Scenario: 선물을 전달하면 재고가 수량만큼 감소한다
  Given "보내는사람"과 "받는사람" 회원이 등록되어 있다
  And "옵션A" 옵션의 재고가 10개 있다
  When "보내는사람"이 "옵션A" 3개를 "받는사람"에게 선물한다
  Then 응답 상태코드가 200이다
  And "옵션A" 옵션의 재고가 7개이다
```

- **Given**: 사전 조건 설정
- **When**: 사용자 행위
- **Then**: 기대 결과

HTTP 메서드나 JSON 같은 구현 세부사항이 안 보인다. 비즈니스 규칙만 드러나기 때문에 비개발자도 읽을 수 있다.

### Step Definitions와 파라미터 추출

Feature 파일의 각 줄은 Step Definition 메서드에 매핑된다.

```java
@When("{string}이 {string} {int}개를 {string}에게 선물한다")
public void 선물한다(String sender, String option, int qty, String receiver) {
    // ...
}
```

`{string}`, `{int}` 같은 플레이스홀더로 파라미터를 추출한다. 한글 Step도 `io.cucumber.java.ko` 패키지로 지원된다.

### Spring 통합과 시나리오 격리

`@CucumberContextConfiguration`을 붙인 클래스가 Spring Boot 테스트 컨텍스트를 설정한다.

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration { ... }
```

`@Before` hook은 **매 시나리오 실행 전**에 호출된다. 여기서 DB를 TRUNCATE하고 공유 상태를 초기화해서, 시나리오끼리 데이터가 섞이지 않게 한다.

### 시나리오 간 데이터 공유

Given에서 생성한 ID를 When에서 써야 하므로, static 필드로 Step 간 데이터를 공유했다.

```java
static Map<String, Long> memberIds = new HashMap<>();
static Map<String, Long> categoryIds = new HashMap<>();
```

`@Before`에서 `reset()`을 호출해 매 시나리오마다 깨끗하게 시작한다.

### ApiClient 패턴

Step Definition에서 RestAssured를 직접 쓰지 않고 ApiClient 클래스로 분리했다. API가 바뀌면 ApiClient 내부만 수정하면 되고, Feature 파일이나 Step Definition은 건드릴 필요 없다.

---

## 2단계: PostgreSQL + Docker Compose

### Production Parity

H2와 PostgreSQL은 같은 SQL처럼 보이지만 실제로 다른 점이 많다. H2의 `SET REFERENTIAL_INTEGRITY FALSE`는 PostgreSQL에 없고, TRUNCATE 문법도 다르다. 프로덕션에서 PostgreSQL을 쓴다면 테스트도 PostgreSQL을 써야 신뢰할 수 있다.

### Docker Compose로 환경 코드화

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test -d gift_test"]
      interval: 2s
      timeout: 5s
      retries: 10
```

이 파일 하나로 누구든 동일한 PostgreSQL 환경을 만들 수 있다.

### Health Check

컨테이너가 **시작**된 것과 **준비**된 것은 다르다. `pg_isready`로 PostgreSQL이 실제로 쿼리를 받을 수 있는지 확인한다. `docker compose up -d --wait`의 `--wait`이 모든 서비스가 healthy가 될 때까지 기다려준다.

### Spring Profile 분리

`application-cucumber.properties`에 PostgreSQL 설정을 넣고, `@ActiveProfiles("cucumber")`로 Cucumber 테스트에서만 활성화했다. `./gradlew test`는 H2, `./gradlew cucumberTest`는 PostgreSQL로 각각 독립 실행된다.

### Gradle 태스크 자동화

```gradle
tasks.register('cucumberTest', Test) {
    dependsOn 'bootJar'
    doFirst {
        exec { commandLine 'sh', '-c', 'docker compose build' }
        exec { commandLine 'sh', '-c', 'docker compose up -d --wait' }
    }
    finalizedBy 'dockerDown'
}
```

`finalizedBy`는 try-finally처럼 테스트가 실패해도 `dockerDown`이 반드시 실행된다. `excludeEngines 'cucumber'`로 일반 `test` 태스크에서 Cucumber 엔진을 제외하여 태스크를 분리했다.

---

## 3단계: Application 컨테이너화

### 왜 앱도 Docker에?

2단계에서 DB는 Docker인데 앱은 로컬 JVM이었다. 프로덕션에서는 앱도 컨테이너에서 실행되니까, 진정한 E2E 테스트라면 앱도 Docker에 넣어야 한다.

### 아키텍처

```
테스트 (Host) → HTTP → localhost:28080 (Docker App)
테스트 (Host) → JDBC → localhost:5432  (Docker DB)
App (Container) → JDBC → postgres:5432  (Docker DB)
```

테스트만 호스트에서 실행되고, 앱과 DB는 Docker 안에서 동작한다.

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

JDK가 아니라 JRE를 사용했다. 런타임에 컴파일러가 필요 없으니 JRE면 충분하고 이미지도 더 작다. jar 빌드는 호스트에서 `./gradlew bootJar`로 하고, 결과물만 복사한다.

### .dockerignore

`docker build`를 실행하면 Docker는 현재 디렉토리(빌드 컨텍스트)의 모든 파일을 Docker 데몬에게 전송한다. `.git`, `.gradle`, `build/classes` 같은 불필요한 파일까지 전부 보내면 빌드가 느려지고 이미지 크기도 커질 수 있다. `.dockerignore`에 제외할 파일을 적으면 빌드 컨텍스트에서 빠진다.

주의할 점은, `build` 디렉토리 전체를 제외하면 `COPY build/libs/*.jar`도 실패한다는 것이다. 그래서 `build/classes`, `build/reports` 같은 하위 디렉토리만 제외하고 `build/libs`는 포함되게 했다.

### Docker 네트워크

Docker Compose는 같은 네트워크의 서비스끼리 **service name을 hostname으로** 사용할 수 있게 해준다. 그래서 앱 컨테이너에서 DB에 접속할 때 `localhost`가 아니라 `postgres`를 쓴다.

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
```

반면 호스트(테스트)에서는 Docker 네트워크 밖이라 `localhost:5432`로 접속한다.

### depends_on + service_healthy

```yaml
app:
  depends_on:
    postgres:
      condition: service_healthy
```

`depends_on`만 쓰면 시작 순서만 보장한다. `condition: service_healthy`를 붙여야 PostgreSQL이 실제로 준비된 후에 앱이 시작된다.

### webEnvironment = NONE

앱이 Docker에서 실행되니까 테스트 프로세스에서 임베디드 서버를 띄울 필요가 없다. `NONE`으로 설정하고 RestAssured가 `localhost:28080`으로 직접 요청을 보낸다. `@LocalServerPort`도 필요 없어진다.

### 포트 매핑 (28080:8080)

앱 컨테이너 내부 8080을 호스트의 28080에 매핑했다. 로컬에서 다른 서버가 8080을 쓸 수 있으니 충돌 방지 목적이다.

---

## 과정에서 배운 것들

- **H2와 PostgreSQL의 SQL 차이**: H2의 `SET REFERENTIAL_INTEGRITY FALSE`는 PostgreSQL에 없다. PostgreSQL에서는 `TRUNCATE TABLE ... CASCADE`로 외래 키 의존 관계를 한 번에 처리할 수 있다. 같은 SQL이라도 DB마다 문법이 다를 수 있으니 실제 프로덕션 DB로 테스트하는 게 중요하다.
- **Docker 안에서 빌드 vs 호스트에서 빌드**: 처음에는 Dockerfile에서 Gradle 빌드까지 하는 multi-stage build를 시도했다. 그런데 Apple Silicon + Alpine 조합에서 Gradle Daemon이 크래시하는 문제가 있었다. 호스트에서 jar를 빌드하고 결과물만 Docker에 복사하는 방식이 더 안정적이었다. 환경에 따라 유연하게 접근해야 한다는 걸 배웠다.
- **`finalizedBy`의 중요성**: 테스트가 실패하면 뒤에 있는 `dockerDown`이 실행 안 될 수도 있다고 생각했는데, `finalizedBy`는 try-finally처럼 성공/실패 관계없이 항상 실행된다. 컨테이너가 방치되는 걸 막아준다.

---

## 최종 실행

```bash
./gradlew test            # JUnit 단위 테스트 (H2)
./gradlew cucumberTest    # Cucumber 인수 테스트 (Docker: PostgreSQL + App)
```

`cucumberTest` 하나로 jar 빌드 → Docker 이미지 생성 → 컨테이너 시작 → 테스트 실행 → 정리까지 전부 자동으로 돌아간다. Docker만 설치되어 있으면 누구든 한 줄로 전체 E2E 테스트를 실행할 수 있다.
