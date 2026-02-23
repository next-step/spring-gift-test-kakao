# 인수 테스트 체계 고도화 가이드 (2단계)

## 개요

1단계(RestAssured 기반)를 Cucumber BDD + PostgreSQL + Docker 환경으로 전환한다.

**핵심 가치:**
- **재현 가능성** — 다른 개발자도 동일한 결과
- **Production Parity** — 실제 배포 환경과 동일한 구조
- **자동화** — 환경 준비부터 정리까지 자동 처리
- **격리** — 로컬 환경 오염 없이 독립 실행

**3단계 순차 진행:**
1. Cucumber BDD 적용
2. PostgreSQL + Docker Compose 통합
3. Application 컨테이너화

---

## 요구사항 1: Cucumber BDD 적용

### 목표
RestAssured 기반 인수 테스트를 Cucumber BDD 형식으로 전환한다.

### Gherkin 작성 원칙

**도메인 언어로 표현:**
```gherkin
# ✅ 비즈니스 언어
Scenario: 재고가 1개인 옵션에 선물을 2번 시도
  Given "아이폰 128GB" 옵션의 재고가 1개 있다
  When 회원 1번이 "아이폰 128GB" 1개를 선물한다
  Then 선물 발송이 성공한다
  When 다시 선물을 시도한다
  Then 재고 부족으로 실패한다

# ❌ 기술 용어 노출
Scenario: POST /api/gifts 호출 시 200 응답
```

### ScenarioContext 패턴

```java
@ScenarioScope
public class ScenarioContext {
    private final Map<String, Object> context = new HashMap<>();

    public void set(String key, Object value) {
        context.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(context.get(key));
    }
}
```

### Step Definition 예시

```java
public class GiftStepDefinitions {
    @Given("{string} 옵션의 재고가 {int}개 있다")
    public void 옵션_재고_설정(String optionName, int quantity) {
        Long optionId = testDataBuilder.createOption(optionName, quantity);
        context.set("optionId", optionId);
    }

    @When("회원 {long}번이 {string} {int}개를 선물한다")
    public void 선물_보내기(Long fromId, String optionName, int qty) {
        Long optionId = context.get("optionId", Long.class);
        Response response = apiClient.sendGift(fromId, optionId, qty);
        context.set("lastResponse", response);
    }
}
```

### 핵심 키워드
- `io.cucumber:cucumber-spring` — Spring 통합
- `@CucumberContextConfiguration` — Spring Boot 설정
- `@ScenarioScope` — 시나리오별 Bean 생성
- `io.cucumber.java.ko` — 한글 Step Definitions
- Feature 파일: `src/test/resources/features/`
- JUnit Platform Suite API

### 검증
```bash
./gradlew test
```

---

## 요구사항 2: PostgreSQL + Docker Compose 통합

### 목표
H2를 PostgreSQL로 전환하고, Docker Compose로 테스트 환경을 자동화한다.

### 왜 H2 대신 PostgreSQL인가?
- SQL 방언 차이로 CI에서 실패하는 문제 방지
- flush/트랜잭션 동작 차이 제거
- "내 컴에선 되는데" 문제 해결

### Docker Compose 구성

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test"]
      interval: 5s
      timeout: 3s
      retries: 10
```

### Spring 프로파일 분리
- `application-cucumber.properties` — PostgreSQL 설정
- `@ActiveProfiles("cucumber")` — 테스트에서 활성화

### 네트워크
- 테스트 코드: Host에서 실행 → `localhost:5432`로 DB 접근
- PostgreSQL: Docker 컨테이너에서 실행

### Gradle Task 자동화
- `doFirst` — 테스트 전 Docker Compose up
- `finalizedBy` — 테스트 후 Docker Compose down (실패 시에도 정리)

### DatabaseCleaner 변경 포인트
- H2의 `SET REFERENTIAL_INTEGRITY FALSE` → PostgreSQL에서는 다른 방식 필요
- `TRUNCATE ... CASCADE` 또는 FK disable 방식 검토

### Testcontainers vs Docker Compose vs H2

| 방식 | 장점 | 단점 |
|------|------|------|
| Testcontainers | Java 코드 제어, JUnit 통합 | 복잡한 환경은 장황 |
| Docker Compose | 선언적, 다중 서비스 쉬움 | Java와 분리 |
| H2 in-memory | 가장 빠름 | 프로덕션과 다름 |

### 핵심 키워드
- `healthcheck` / `pg_isready`
- `@ActiveProfiles("cucumber")`
- `application-cucumber.properties`
- Gradle `Exec` task / `doFirst` / `finalizedBy`

### 검증
```bash
./gradlew cucumberTest
```

---

## 요구사항 3: Application 컨테이너화

### 목표
Spring Boot 앱까지 Docker 컨테이너로 실행하여 E2E 테스트를 수행한다.

### 아키텍처

```
테스트 (Host) → HTTP → localhost:28080 (Docker App)
테스트 (Host) → JDBC → localhost:5432  (Docker DB)
App (Container) → JDBC → postgres:5432  (Docker DB)
```

### Multi-stage Dockerfile

```dockerfile
# Builder stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose에 앱 서비스 추가

```yaml
services:
  app:
    build: .
    ports:
      - "28080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/testdb
```

### 핵심 포인트
- `depends_on: condition: service_healthy` — DB 준비 후 앱 시작
- Docker 네트워크에서 service name(`postgres`)이 hostname
- 컨테이너 내부: `postgres:5432` / 테스트(Host): `localhost:28080`
- `webEnvironment = NONE` — embedded 서버 제거 (Docker 앱에 HTTP 요청)
- `.dockerignore` — 불필요한 파일 제외

### 검증
```bash
./gradlew dockerBuild
./gradlew dockerUp
curl http://localhost:28080
./gradlew cucumberTest
./gradlew dockerDown
```

---

## Test Doubles 전략

| Type | 목적 | 사용 시점 |
|------|------|-----------|
| Dummy | 파라미터 채우기 | 사용하지 않는 의존성 |
| Stub | 미리 정한 응답 반환 | 실패 시나리오 테스트 |
| Mock | 호출 검증 | 단위 테스트에서 상호작용 검증 |
| Fake | 간단한 실제 구현 | 인수 테스트에서 외부 의존 격리 |

### 인수 테스트 → Fake

```java
@Component
@Profile("test")
public class FakeGiftDelivery implements GiftDelivery {
    @Override
    public void deliver(Gift gift) {
        System.out.println("선물 배송: " + gift);
    }
}
```

### 실패 시나리오 → Stub

```java
public class StubGiftDelivery implements GiftDelivery {
    private boolean shouldFail = false;
    public void setShouldFail(boolean fail) { this.shouldFail = fail; }

    @Override
    public void deliver(Gift gift) {
        if (shouldFail) throw new DeliveryException("배송 실패");
    }
}
```

---

## 리스크 기반 AC 정의

### 공식
```
Risk = Impact × Likelihood × Detection Cost
```

| Risk | 전략 |
|------|------|
| High | Cucumber 자동화 필수 |
| Medium | 선택적 자동화 또는 수동 |
| Low | 수동 QA만 |

### AC 정의 템플릿
```markdown
## User Story
As a [역할]
I want to [행동]
So that [가치]

### AC1: [시나리오] ✅ 자동화
- Risk: High (이유)

### AC2: [시나리오] ❌ 수동
- Risk: Low (이유)
```

---

## 트러블슈팅

| 명령 | 용도 |
|------|------|
| `docker ps` | 컨테이너 상태 확인 |
| `docker logs <container>` | 로그 확인 |
| `docker exec -it <container> bash` | 컨테이너 내부 진입 |
| `docker system prune` | 캐시 정리 |

---

## 참고 링크
- [Cucumber Documentation](https://cucumber.io/docs)
- [Cucumber-Spring Integration](https://cucumber.io/docs/cucumber/state/#spring)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker Compose Networking](https://docs.docker.com/compose/networking/)
