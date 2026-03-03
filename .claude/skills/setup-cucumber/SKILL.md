---
name: setup-cucumber
description: Cucumber BDD 인프라를 구성한다. 의존성 추가, 테스트 러너, Spring 통합, 기존 테스트를 Gherkin 시나리오로 전환한다.
disable-model-invocation: true
argument-hint: (인자 없음)
---

Cucumber BDD 인프라를 구성한다. 아래 단계를 순서대로 수행한다.

## 1단계: 의존성 추가

build.gradle에 Cucumber 관련 의존성을 추가한다.

필요한 의존성:
- `io.cucumber:cucumber-java` — Step Definitions 작성
- `io.cucumber:cucumber-spring` — Spring Boot 통합
- `io.cucumber:cucumber-junit-platform-engine` — JUnit Platform에서 실행
- `org.junit.platform:junit-platform-suite` — Suite API로 Cucumber 실행

한글 Step Definitions(`조건`, `먼저`, `만약`, `그러면`)을 사용하려면 `io.cucumber.java.ko` 패키지를 import한다.

## 2단계: Cucumber 테스트 러너 생성

`src/test/java/gift/acceptance/` 에 CucumberTest 클래스를 생성한다.

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("gift.acceptance")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.acceptance")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class CucumberTest {
}
```

## 3단계: Spring 통합 설정

`src/test/java/gift/acceptance/` 에 CucumberSpringConfiguration 클래스를 생성한다.

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }
}
```

- `@CucumberContextConfiguration`으로 Spring 컨텍스트를 Cucumber에 연결한다
- `@Before`는 `io.cucumber.java.Before`를 사용한다 (JUnit의 @BeforeEach가 아님)

## 4단계: Feature 파일 디렉토리 생성

`src/test/resources/features/` 디렉토리를 생성한다.

## 5단계: 시나리오 간 상태 공유 Bean

Step Definitions 간 HTTP 응답을 공유할 Bean을 생성한다.

```java
@Component
@ScenarioScope
public class SharedContext {
    private Response response;

    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }
}
```

- `@ScenarioScope`로 시나리오마다 새 인스턴스를 생성하여 격리한다
- Step Definition 클래스들이 이 Bean을 주입받아 사용한다

## 6단계: 데이터 격리

Cucumber에서는 `@Sql` 어노테이션을 사용할 수 없다. 대신 `@Before` hook에서 데이터를 초기화한다.

```java
@Before
public void cleanup() {
    // JdbcTemplate 또는 Repository를 사용하여 테이블 초기화
}
```

또는 각 시나리오의 `배경` (Background) 블록에서 데이터 상태를 명시한다.

## 7단계: 기존 테스트 전환

기존 RestAssured 기반 인수 테스트를 Feature 파일 + Step Definitions로 전환한다.

전환 순서:
1. 기존 테스트의 @DisplayName을 시나리오 제목으로 변환
2. 테스트 로직을 Given/When/Then으로 분리
3. Step Definition 메서드를 작성
4. 기존 테스트 클래스는 전환 완료 후 제거

## 8단계: 검증

```bash
./gradlew test
```

- 모든 Cucumber 시나리오가 실행되고 통과하는지 확인한다
- `pretty` 플러그인으로 한글 시나리오가 콘솔에 출력되는지 확인한다

## 주의사항

- Feature 파일은 `.feature` 확장자를 사용한다
- 한글 Gherkin 사용 시 파일 첫 줄에 `# language: ko`를 명시한다
- Step Definition 파라미터 추출은 Cucumber Expressions 또는 정규식을 사용한다
- 하나의 Step Definition 클래스가 너무 커지지 않도록 도메인별로 분리한다
