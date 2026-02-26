# Step 1 - Cucumber 전환 요구사항 충족 여부

## 요구사항 체크리스트

| # | 요구사항 | 충족 여부 |
|---|---------|----------|
| 1 | Gherkin 한글 시나리오 | X |
| 2 | Cucumber-Spring 통합 | O |
| 3 | Step Definitions | O |
| 4 | 데이터 격리 | O |

---

## 1. Gherkin 한글 시나리오

**충족 여부**: X

### 미충족 사유

Given-When-Then 키워드를 한글로 전환하지 않았다. Feature 파일에서 시나리오명과 스텝 본문은 한글이지만, 키워드는 영문 `Given`, `When`, `Then`을 그대로 사용한다.

한글 키워드(`주어진`, `만일`, `그러면`)로 전환할 수 있으나, 오히려 가독성이 떨어진다고 판단하여 의도적으로 적용하지 않았다. 영문 키워드는 Gherkin의 표준 문법으로서 개발자에게 이미 익숙하며, 한글 본문과 자연스럽게 조합된다.

### 현재 상태

3개의 `.feature` 파일에 총 13개의 시나리오가 작성되어 있으며, 키워드를 제외한 시나리오명과 스텝 본문은 모두 한글이다.

```gherkin
# 현재 (영문 키워드 + 한글 본문)
Given "테스트 카테고리" 카테고리가 등록되어 있다
When 보내는 회원이 3개 수량으로 선물을 전송하면
Then 응답 상태 코드는 200이다

# 한글 키워드 전환 시 (가독성 저하)
주어진 "테스트 카테고리" 카테고리가 등록되어 있다
만일 보내는 회원이 3개 수량으로 선물을 전송하면
그러면 응답 상태 코드는 200이다
```

---

## 2. Cucumber-Spring 통합

**충족 여부**: O

### 근거

#### 설정 클래스 (`CucumberSpringConfiguration.java`)

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class Config {

        @Bean
        @Scope("cucumber-glue")
        public ScenarioContext scenarioContext() {
            return new ScenarioContext();
        }

        @Bean
        @Scope("cucumber-glue")
        public ApiClient apiClient(@Value("${local.server.port}") int port) {
            return new ApiClient(port);
        }
    }
}
```

- `@CucumberContextConfiguration`으로 Cucumber와 Spring 컨텍스트를 연결한다.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 실제 서버를 띄워 인수 테스트를 수행한다.
- `@Scope("cucumber-glue")`로 빈의 생명주기를 Cucumber 시나리오와 일치시킨다.

#### 테스트 러너 (`CucumberTest.java`)

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.acceptance")
public class CucumberTest {
}
```

- JUnit Platform Suite를 통해 Cucumber 엔진을 실행한다.
- `features` 디렉토리의 `.feature` 파일을 자동으로 로드한다.

#### 의존성 (`build.gradle`)

```gradle
testImplementation 'io.cucumber:cucumber-java:7.20.1'
testImplementation 'io.cucumber:cucumber-spring:7.20.1'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.20.1'
testImplementation 'org.junit.platform:junit-platform-suite'
```

- Cucumber 7.20.1과 Spring 통합 모듈이 올바르게 선언되어 있다.

---

## 3. Step Definitions

**충족 여부**: O

### 근거

4개의 Step Definition 클래스에 총 27개의 스텝 메서드가 구현되어 있다.

| 클래스 | 역할 | 스텝 수 |
|--------|------|--------|
| `CommonSteps.java` | 데이터 정리, 공통 검증 (`응답 상태 코드는 {int}이다`) | 2개 |
| `CategorySteps.java` | 카테고리 생성/조회 스텝 | 6개 |
| `ProductSteps.java` | 상품 생성/조회 스텝, 카테고리 전제 조건 | 9개 |
| `GiftSteps.java` | 선물 전송, 재고 검증, 회원/옵션 전제 조건 | 10개 |

### 구현 특징

- `@Given`, `@When`, `@Then` 어노테이션에 한글 표현식을 사용한다.
- `ScenarioContext`를 통해 스텝 간 상태(ID, 응답)를 공유한다.
- `ApiClient`로 HTTP 요청 로직을 추상화하여 스텝 코드의 가독성을 높인다.
- AssertJ를 사용하여 검증한다.

### 예시

```java
// GiftSteps.java
@When("보내는 회원이 {int}개 수량으로 선물을 전송하면")
public void 선물을_전송하면(int quantity) { ... }

@Then("옵션 재고가 {int}개로 차감되어 있다")
public void 옵션_재고가_차감되어_있다(int expectedQuantity) {
    Option option = optionRepository.findById(context.getOptionId()).orElseThrow();
    assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
}
```

---

## 4. 데이터 격리

**충족 여부**: O

### 근거

세 가지 메커니즘으로 시나리오 간 데이터 격리를 보장한다.

#### 4-1. `@Before` 훅을 통한 데이터 초기화

```java
// CommonSteps.java
@Before
public void cleanUp() {
    optionRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    memberRepository.deleteAll();
}
```

- 각 시나리오 실행 전에 모든 테이블의 데이터를 삭제한다.
- 외래 키 제약 조건을 고려하여 자식 테이블부터 삭제한다 (Option -> Product -> Category, Member).

#### 4-2. `ScenarioContext`의 `cucumber-glue` 스코프

```java
@Bean
@Scope("cucumber-glue")
public ScenarioContext scenarioContext() {
    return new ScenarioContext();
}
```

- `ScenarioContext`는 시나리오마다 새로운 인스턴스가 생성된다.
- 이전 시나리오에서 저장한 ID나 응답이 다음 시나리오에 영향을 주지 않는다.

#### 4-3. `RANDOM_PORT`를 통한 포트 격리

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
```

- 테스트 실행 시 동적으로 포트를 할당하여 다른 프로세스와 충돌하지 않는다.
