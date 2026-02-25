# Cucumber BDD 학습 가이드

이 문서는 프로젝트에 적용한 Cucumber BDD 인수 테스트를 직접 작성하기 위해 필요한 개념과 구현 방법을 정리한다.

---

## 1. Cucumber란?

Cucumber는 **Gherkin** 문법으로 작성된 시나리오(Feature 파일)를 실행 가능한 테스트로 변환해주는 BDD 프레임워크다.

```
Feature 파일 (.feature)  →  Step Definitions (Java)  →  테스트 실행
     비즈니스 언어              코드 매핑                  실제 검증
```

핵심 가치: **비개발자도 읽을 수 있는 테스트 시나리오**를 작성하면서, 그것이 곧 실행 가능한 자동화 테스트가 된다.

---

## 2. Gradle 의존성

```groovy
testImplementation 'io.cucumber:cucumber-java:7.21.1'              // Gherkin 파싱 + Step 어노테이션
testImplementation 'io.cucumber:cucumber-spring:7.21.1'            // Spring DI 통합
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.21.1'  // JUnit 5 엔진
testImplementation 'org.junit.platform:junit-platform-suite'       // @Suite 러너
```

| 라이브러리 | 역할 |
|-----------|------|
| `cucumber-java` | `@Given`, `@When`, `@Then` 어노테이션과 Gherkin 파서 제공 |
| `cucumber-spring` | `@CucumberContextConfiguration`, `@ScenarioScope` 등 Spring 통합 |
| `cucumber-junit-platform-engine` | JUnit Platform에서 Cucumber 테스트를 실행하는 엔진 |
| `junit-platform-suite` | `@Suite`로 Cucumber 엔진을 JUnit에서 구동 |

---

## 3. Gherkin 문법 (Feature 파일)

### 기본 구조

```gherkin
# language: ko
기능: 선물하기

  배경:
    조건 "식품" 카테고리가 있다
    그리고 "식품" 카테고리에 "케이크" 상품이 있다

  시나리오: 정상 선물 보내기
    조건 "케이크" 상품에 "기본" 옵션의 재고가 10개 있다
    만일 "보내는사람"이 "받는사람"에게 "기본" 옵션 3개를 선물한다
    그러면 선물이 성공한다
    그리고 "기본" 옵션의 재고가 7개이다
```

### 핵심 키워드

| 한글 키워드 | 영문 | 용도 |
|-----------|------|------|
| `기능` | Feature | 테스트 그룹의 제목 |
| `배경` | Background | 모든 시나리오 전에 공통 실행되는 전제 조건 |
| `시나리오` | Scenario | 하나의 테스트 케이스 |
| `조건` | Given | 사전 상태 설정 |
| `만일` | When | 사용자 행위 (테스트 대상) |
| `그러면` | Then | 기대 결과 검증 |
| `그리고` | And | 앞 키워드의 연장 (Given이면 Given, Then이면 Then) |

### `# language: ko`

Feature 파일 첫 줄에 반드시 `# language: ko`를 넣어야 한글 키워드를 인식한다.

### 배경(Background) vs 시나리오

- **배경**: 모든 시나리오에 공통으로 실행. 반복되는 Given을 추출할 때 사용
- **시나리오**: 독립적인 하나의 테스트 케이스. 각 시나리오는 배경 + 자기 자신의 스텝을 순서대로 실행

```
시나리오 1 실행 순서: 배경 → 시나리오 1의 스텝들
시나리오 2 실행 순서: 배경 → 시나리오 2의 스텝들
```

### 파라미터

Feature 파일에서 `"식품"`, `10` 같은 값은 Step Definition에서 파라미터로 받는다.

```gherkin
조건 "케이크" 상품에 "기본" 옵션의 재고가 10개 있다
```
```java
@조건("{string} 상품에 {string} 옵션의 재고가 {int}개 있다")
public void 옵션_재고_설정(String productName, String optionName, int quantity) {
```

| Gherkin 표현 | Java 타입 | 매칭 |
|-------------|----------|------|
| `{string}` | `String` | `"큰따옴표"` 안의 문자열 |
| `{int}` | `int` | 정수 |
| `{long}` | `long` | Long 정수 |
| `{float}` | `float` | 소수 |

---

## 4. 프로젝트 구조

```
src/test/java/gift/cucumber/
├── CucumberTest.java             # ① JUnit Suite 러너
├── CucumberSpringConfig.java     # ② Spring Boot 통합 설정
├── CucumberHooks.java            # ③ 시나리오 전후 처리
├── ScenarioContext.java           # ④ Step 간 상태 공유
└── steps/                         # ⑤ Step Definitions
    ├── GiftStepDefinitions.java
    ├── CategoryStepDefinitions.java
    └── ProductStepDefinitions.java

src/test/resources/features/       # ⑥ Feature 파일
├── gift.feature
├── category.feature
└── product.feature
```

---

## 5. 각 파일의 역할과 작성법

### ① CucumberTest.java — 러너

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("gift.cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.cucumber")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class CucumberTest {
}
```

**학습 포인트:**
- `@Suite` + `@IncludeEngines("cucumber")` — JUnit Platform에서 Cucumber 엔진을 실행
- `GLUE_PROPERTY_NAME` — Step Definitions와 Hooks를 찾을 패키지
- `FEATURES_PROPERTY_NAME` — Feature 파일 경로
- `PLUGIN_PROPERTY_NAME` — `"pretty"`는 실행 결과를 보기 좋게 출력
- **이 클래스 자체에는 코드가 없다.** 설정만으로 Cucumber를 구동하는 진입점 역할

### ② CucumberSpringConfig.java — Spring 통합

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {
}
```

**학습 포인트:**
- `@CucumberContextConfiguration` — Cucumber에게 "이 클래스가 Spring 설정이다"를 알려줌
- `@SpringBootTest(RANDOM_PORT)` — 실제 서블릿 컨테이너를 띄움 (기존 RestAssured 테스트와 동일)
- glue 패키지 내에 **반드시 하나만** 있어야 한다 (두 개 이상이면 에러)
- `public`이어야 한다

### ③ CucumberHooks.java — 시나리오 전후 처리

```java
public class CucumberHooks {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Before  // io.cucumber.java.Before (JUnit의 @BeforeEach가 아님!)
    public void setUp() {
        RestAssured.port = port;
        databaseCleaner.clear();
    }
}
```

**학습 포인트:**
- `io.cucumber.java.Before` — **각 시나리오 실행 전에** 호출됨 (`@BeforeEach`와 같은 역할)
- `io.cucumber.java.After` — 시나리오 후 정리 작업 (필요 시)
- `@Autowired`가 동작한다 — `CucumberSpringConfig`가 Spring 컨텍스트를 제공하므로
- `@LocalServerPort`도 동작한다 — 실제 서버의 랜덤 포트를 주입받음

**주의: import 경로**
```java
import io.cucumber.java.Before;    // ✅ Cucumber의 Before
import org.junit.jupiter.api.BeforeEach;  // ❌ JUnit의 BeforeEach (Cucumber에서 동작 안 함)
```

### ④ ScenarioContext.java — Step 간 상태 공유

```java
@Component
@ScenarioScope
public class ScenarioContext {
    private final Map<String, Object> context = new HashMap<>();
    private Response lastResponse;

    public void set(String key, Object value) { ... }
    public <T> T get(String key) { ... }
    public Response getLastResponse() { ... }
    public void setLastResponse(Response response) { ... }
}
```

**학습 포인트:**

**왜 필요한가?**
Cucumber에서 Given/When/Then은 **서로 다른 메서드**다. 하나의 시나리오 안에서 Given에서 생성한 데이터를 When에서 사용하고, When의 응답을 Then에서 검증해야 한다. 이 메서드들은 같은 클래스에 있을 수도 있고, 다른 클래스에 있을 수도 있다.

**`@ScenarioScope`가 핵심**
- 시나리오마다 **새로운 인스턴스**가 생성됨
- 시나리오가 끝나면 자동으로 폐기됨
- 따라서 시나리오 간 상태가 오염되지 않는다

**네이밍 컨벤션**
```java
context.set("category:식품", category);     // "타입:이름" 형식으로 키를 정해 충돌 방지
context.set("member:보내는사람", sender);
context.set("option:기본", option);
```

**`@ScenarioScope` vs `@Scope("prototype")`**
- `@ScenarioScope`는 Cucumber 시나리오 생명주기에 맞춤
- `prototype`은 주입할 때마다 새 인스턴스 → Step Definition마다 다른 객체를 받게 됨 (사용하면 안 됨)

### ⑤ Step Definitions — 실제 로직

```java
public class GiftStepDefinitions {

    @Autowired
    ScenarioContext context;

    @Autowired
    OptionRepository optionRepository;

    @조건("{string} 상품에 {string} 옵션의 재고가 {int}개 있다")
    public void 옵션_재고_설정(String productName, String optionName, int quantity) {
        var saved = optionRepository.save(option(quantity, context.get("product:" + productName)));
        context.set("option:" + optionName, saved);
    }

    @만일("{string}이 {string}에게 {string} 옵션 {int}개를 선물한다")
    public void 선물을_보낸다(String senderName, String receiverName, String optionName, int quantity) {
        Member sender = context.get("member:" + senderName);
        Member receiver = context.get("member:" + receiverName);
        Option opt = context.get("option:" + optionName);

        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(/* JSON */)
                .when()
                .post("/api/gifts");

        context.setLastResponse(response);  // Then에서 사용
    }

    @그러면("선물이 성공한다")
    public void 선물이_성공한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(200);
    }
}
```

**학습 포인트:**

**한글 어노테이션**
```java
import io.cucumber.java.ko.조건;   // Given
import io.cucumber.java.ko.만일;   // When
import io.cucumber.java.ko.그러면;  // Then
import io.cucumber.java.ko.그리고;  // And
```
`io.cucumber.java.ko` 패키지에서 한글 키워드에 대응하는 어노테이션을 제공한다.

**Step은 Feature 파일의 문장과 1:1 매핑**
```
Feature:  조건 "케이크" 상품에 "기본" 옵션의 재고가 10개 있다
Step:     @조건("{string} 상품에 {string} 옵션의 재고가 {int}개 있다")
```
문장이 정확히 일치해야 한다. 한 글자라도 다르면 "undefined step" 에러가 발생한다.

**Step은 전역(global)이다**
- 같은 문장은 어떤 Feature 파일에서 사용하든 같은 Step 메서드를 호출한다
- 따라서 같은 패턴의 Step을 두 클래스에 중복 정의하면 에러가 난다 (Ambiguous step)
- 예: `"등록이 성공한다"`를 CategoryStepDefinitions에 정의하면 product.feature에서도 쓸 수 있다

**Step 클래스 분리 기준**
- 도메인 행위 단위로 분리: `GiftStepDefinitions`, `CategoryStepDefinitions`, `ProductStepDefinitions`
- 공통 검증 스텝(`등록이 성공한다`, `응답 목록이 비어있다`)은 가장 먼저 필요한 클래스에 둔다

---

## 6. 실행 흐름 요약

```
1. ./gradlew test 실행
2. JUnit Platform이 CucumberTest(@Suite)를 발견
3. Cucumber 엔진이 features/ 폴더의 .feature 파일을 파싱
4. CucumberSpringConfig을 통해 Spring Boot 컨텍스트 기동
5. 각 시나리오마다:
   a. CucumberHooks.@Before → port 설정 + DB 초기화
   b. ScenarioContext 새로 생성 (@ScenarioScope)
   c. Feature의 각 줄 → 매칭되는 Step Definition 메서드 실행
   d. 모든 스텝 통과 → 시나리오 PASSED / 하나라도 실패 → FAILED
6. 결과 출력 (pretty 플러그인)
```

---

## 7. 자주 만나는 에러와 해결법

### "No backends were found" 또는 시나리오가 아예 실행 안 됨
- `GLUE_PROPERTY_NAME` 경로가 Step Definition 패키지와 일치하는지 확인
- `CucumberSpringConfig`이 glue 패키지 안에 있는지 확인

### "Undefined step"
- Feature 파일의 문장과 `@조건`/`@만일`/`@그러면`의 패턴이 정확히 일치하는지 확인
- `{string}`은 `"큰따옴표"`가 필수. Feature에서 따옴표를 빼먹으면 매칭 안 됨

### "Ambiguous step definitions"
- 같은 패턴이 두 개 이상의 메서드에 정의됨
- 하나를 삭제하거나 패턴을 더 구체적으로 변경

### Step에서 `@Autowired`가 null
- `CucumberSpringConfig`에 `@CucumberContextConfiguration`이 있는지 확인
- Step Definition 클래스가 glue 패키지 안에 있는지 확인

### Feature 파일에서 한글 키워드가 인식 안 됨
- 첫 줄에 `# language: ko`가 있는지 확인

### JSON path 검증 실패 (예: name이 다른 필드에 매칭)
- `body("name", ...)` 같은 공통적인 JSON path를 여러 도메인에서 재사용할 때 주의
- 상품 응답의 `name`과 카테고리 응답의 `name`이 다르므로 스텝 문장을 구분해야 함
- 예: `"응답에 카테고리 이름 ... 포함"` vs `"응답에 상품 이름 ... 포함"` vs `"응답 상품의 카테고리가 ..."`

---

## 8. 기존 RestAssured 테스트와의 비교

| 항목 | RestAssured | Cucumber |
|------|------------|----------|
| 테스트 정의 | Java 코드 | Feature 파일 (Gherkin) + Java (Step) |
| 읽는 대상 | 개발자 | 개발자 + 비개발자 |
| 데이터 준비 | 테스트 메서드 내에서 직접 | Given 스텝에서 |
| 상태 공유 | 메서드 내 로컬 변수 | ScenarioContext 빈 |
| 격리 | `@BeforeEach` | `@Before` (Cucumber Hook) |
| 실행 | JUnit 직접 | JUnit → Cucumber 엔진 → Step 실행 |

---

## 9. 참고 자료

- [Cucumber 공식 문서](https://cucumber.io/docs/cucumber/)
- [Cucumber-Spring 통합](https://cucumber.io/docs/cucumber/state/#spring)
- [Gherkin 문법](https://cucumber.io/docs/gherkin/reference/)
- [Cucumber JUnit Platform Engine](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-junit-platform-engine)
- [io.cucumber.java.ko 패키지 (한글 키워드)](https://javadoc.io/doc/io.cucumber/cucumber-java/latest/io/cucumber/java/ko/package-summary.html)
