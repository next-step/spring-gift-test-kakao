# 요구사항 1: Cucumber BDD 적용 — 구현 가이드

## 개요

기존 RestAssured 기반 인수 테스트를 **Cucumber BDD** 형식으로 전환했다.
핵심 목표는 **비개발자도 읽을 수 있는 테스트 시나리오**를 만드는 것이다.

### Before vs After

**Before** — RestAssured 직접 호출 (개발자만 이해 가능)

```java
RestAssured.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"교환권\"}")
    .when()
        .post("/api/categories")
    .then()
        .statusCode(200)
        .body("id", notNullValue());
```

**After** — Gherkin 시나리오 (누구나 이해 가능)

```gherkin
시나리오: 카테고리 생성
  만일 "교환권" 이름으로 카테고리를 생성하면
  그러면 응답 코드는 200이다
  그리고 응답에 id가 포함되어 있다
```

---

## BDD란?

**BDD(Behavior-Driven Development)** 는 소프트웨어의 **행위(behavior)** 를 자연어로 먼저 정의하고,
그 정의가 곧 자동화된 테스트가 되는 개발 방법론이다.

핵심 구조는 **Given-When-Then** 패턴이다:

| 키워드 | 한글 | 역할 | 예시 |
|--------|------|------|------|
| Given | 조건 | 사전 상태를 준비한다 | "교환권" 카테고리가 존재한다 |
| When | 만일 | 행위를 실행한다 | 카테고리 목록을 조회하면 |
| Then | 그러면 | 결과를 검증한다 | 응답 코드는 200이다 |
| And | 그리고 | 앞 키워드를 이어간다 | 목록에 2개의 항목이 있다 |

---

## 전체 파일 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberSpringConfig.java   ← Spring Boot 통합 설정
│   │   ├── RunCucumberTest.java        ← Cucumber 실행 진입점
│   │   ├── TestContext.java            ← 시나리오 내 공유 상태
│   │   ├── hooks/
│   │   │   └── DatabaseCleanupHook.java ← 시나리오 전 DB 정리
│   │   └── steps/
│   │       ├── CommonSteps.java        ← 공통 검증 (상태코드, 목록 크기)
│   │       ├── CategorySteps.java      ← 카테고리 관련 step
│   │       ├── ProductSteps.java       ← 상품 관련 step
│   │       └── GiftSteps.java          ← 선물하기 관련 step
│   └── model/
│       └── OptionTest.java             ← 단위 테스트 (변경 없음)
└── resources/
    └── features/
        ├── category.feature            ← 카테고리 시나리오 (한글)
        ├── product.feature             ← 상품 시나리오 (한글)
        └── gift.feature                ← 선물하기 시나리오 (한글)
```

데이터 흐름을 그림으로 보면:

```
RunCucumberTest (진입점)
  → Cucumber 엔진이 .feature 파일을 읽는다
    → 한글 step과 매칭되는 Java 메서드를 찾는다 (steps/ 패키지)
      → 각 시나리오 실행 전 DatabaseCleanupHook이 DB를 정리한다
        → Step 메서드가 RestAssured로 HTTP 요청을 보내고 결과를 검증한다
```

---

## 구현 단계별 상세 설명

### 1단계: 의존성 추가 (`build.gradle`)

```groovy
testImplementation 'io.cucumber:cucumber-java:7.20.1'
testImplementation 'io.cucumber:cucumber-spring:7.20.1'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.20.1'
testImplementation 'org.junit.platform:junit-platform-suite'
```

| 라이브러리 | 역할 |
|-----------|------|
| `cucumber-java` | Cucumber 핵심. `@Given`, `@When`, `@Then` 어노테이션과 한글 키워드(`@조건`, `@만일`, `@그러면`) 제공 |
| `cucumber-spring` | Cucumber가 Spring의 DI(의존성 주입)를 사용할 수 있게 해준다. `@Autowired`로 Repository 등을 주입받을 수 있는 이유 |
| `cucumber-junit-platform-engine` | JUnit Platform 위에서 Cucumber를 실행하는 엔진. Gradle의 `useJUnitPlatform()`과 연결된다 |
| `junit-platform-suite` | `@Suite` 어노테이션 제공. 여러 feature 파일을 하나의 테스트 러너로 묶어준다 |

### 왜 4개가 다 필요한가?

```
Gradle → JUnit Platform → cucumber-junit-platform-engine → cucumber-java (step 실행)
                                                          → cucumber-spring (Spring DI)
         junit-platform-suite (진입점 역할)
```

Gradle이 테스트를 실행하면, JUnit Platform이 `cucumber-junit-platform-engine`을 통해 Cucumber를 호출한다.
Cucumber는 `.feature` 파일을 읽고, `cucumber-java`의 어노테이션으로 매칭된 Java 메서드를 실행한다.
이때 `cucumber-spring`이 Spring 컨텍스트를 제공해서 `@Autowired`가 동작한다.

---

### 2단계: Feature 파일 작성

Feature 파일은 **비즈니스 언어로 작성된 테스트 시나리오**다. `.feature` 확장자를 가지며,
Gherkin 문법으로 작성한다.

#### `category.feature` — 가장 기본적인 형태

```gherkin
# language: ko
기능: 카테고리 관리

  시나리오: 카테고리 생성
    만일 "교환권" 이름으로 카테고리를 생성하면
    그러면 응답 코드는 200이다
    그리고 응답에 id가 포함되어 있다
```

- `# language: ko` — Gherkin에게 한글 키워드를 사용한다고 알려준다. 이 줄이 없으면 영어(Given/When/Then)로 인식한다.
- `기능:` — 이 feature 파일이 다루는 기능의 이름. 영어의 `Feature:`에 해당.
- `시나리오:` — 하나의 테스트 케이스. 영어의 `Scenario:`에 해당.
- `만일` / `그러면` / `그리고` — Given/When/Then/And의 한글 버전.

#### `gift.feature` — 배경(Background) 사용

```gherkin
배경:
  조건 "교환권" 카테고리가 존재한다
  그리고 "교환권" 카테고리에 상품이 등록되어 있다
    | name              | price | imageUrl                       |
    | 스타벅스 아메리카노 | 4500  | https://example.com/image.png  |
  그리고 상품에 재고 100개의 "기본 옵션"이 등록되어 있다
  그리고 "보내는사람" 회원이 존재한다
  그리고 "받는사람" 회원이 존재한다
```

- `배경:` — 이 파일의 **모든 시나리오에 공통**으로 실행되는 사전 조건. 영어의 `Background:`에 해당.
  선물하기 시나리오는 카테고리, 상품, 옵션, 회원이 항상 필요하므로 배경으로 뺐다.
- `| name | price | ... |` — **DataTable**. 한 step에 여러 데이터를 넘길 때 표 형태로 작성한다.

#### 한글 Gherkin 키워드 매핑

| 한글 | 영어 | 용도 |
|------|------|------|
| 기능 | Feature | 기능 설명 |
| 배경 | Background | 공통 사전 조건 |
| 시나리오 | Scenario | 테스트 케이스 |
| 조건 | Given | 사전 상태 |
| 만일 | When | 행위 실행 |
| 그러면 | Then | 결과 검증 |
| 그리고 | And | 앞 키워드 연장 |

---

### 3단계: Spring Boot 통합 설정 (`CucumberSpringConfig.java`)

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {
}
```

이 클래스는 **빈 클래스**지만 매우 중요한 역할을 한다:

- `@CucumberContextConfiguration` — Cucumber에게 "이 클래스가 Spring 설정의 진입점"이라고 알려준다.
  Cucumber는 이 어노테이션이 붙은 클래스를 찾아서 Spring 컨텍스트를 부트스트랩한다.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — 실제 내장 톰캣 서버를 랜덤 포트로 띄운다.
  RestAssured가 이 서버에 HTTP 요청을 보내서 테스트한다.

#### 기존 `ApiTest`와의 비교

기존에는 `ApiTest` 추상 클래스가 이 역할을 했다:

```java
// 기존 ApiTest (RestAssured 기반)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiTest {
    @LocalServerPort int port;
    // ... repositories, @BeforeEach cleanup
}
```

Cucumber에서는 `@SpringBootTest` 설정과 테스트 로직이 분리된다.
설정은 `CucumberSpringConfig`에, 정리 로직은 `DatabaseCleanupHook`에, 테스트 로직은 각 Step 클래스에 들어간다.

---

### 4단계: 시나리오 스코프 컨텍스트 (`TestContext.java`)

```java
@Component
@ScenarioScope
public class TestContext {
    private Response lastResponse;
    private Category lastCategory;
    private Product lastProduct;
    private Option lastOption;
    private final Map<String, Member> members = new HashMap<>();
    // getter/setter ...
}
```

#### 왜 필요한가?

Cucumber에서는 하나의 시나리오가 **여러 Step 클래스**에 걸쳐 실행된다.

```
시나리오: 선물하기 성공
  조건 "교환권" 카테고리가 존재한다        → CategorySteps
  그리고 "교환권" 카테고리에 상품이 ...    → ProductSteps
  그리고 상품에 재고 100개의 ...          → GiftSteps
  만일 "보내는사람"이 "받는사람"에게 ...   → GiftSteps
  그러면 응답 코드는 200이다              → CommonSteps
```

`CategorySteps`에서 저장한 카테고리를 `ProductSteps`에서 참조해야 한다.
`GiftSteps`에서 보낸 HTTP 응답을 `CommonSteps`에서 검증해야 한다.

**`TestContext`가 이 중간 다리 역할**을 한다. 모든 Step 클래스가 `TestContext`를 주입받아 데이터를 주고받는다.

#### `@ScenarioScope`의 의미

```
시나리오 A 실행 → TestContext 인스턴스 #1 생성 → 시나리오 A 종료 → 인스턴스 #1 폐기
시나리오 B 실행 → TestContext 인스턴스 #2 생성 → 시나리오 B 종료 → 인스턴스 #2 폐기
```

`@ScenarioScope`는 **시나리오마다 새로운 인스턴스**를 만든다.
덕분에 시나리오 A에서 저장한 Response가 시나리오 B에 영향을 주지 않는다.

만약 `@ScenarioScope`가 없으면(`@Singleton`) 모든 시나리오가 같은 `TestContext`를 공유하게 되어
이전 시나리오의 데이터가 다음 시나리오에 남아 테스트가 깨질 수 있다.

---

### 5단계: DB 정리 Hook (`DatabaseCleanupHook.java`)

```java
public class DatabaseCleanupHook {

    @Autowired private WishRepository wishRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MemberRepository memberRepository;

    @Before  // io.cucumber.java.Before
    public void cleanUp() {
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }
}
```

#### 삭제 순서가 중요하다

외래 키(FK) 제약조건 때문에 **자식 → 부모** 순서로 삭제해야 한다:

```
wish (→ member, product)     ← 1번째 삭제
option (→ product)           ← 2번째 삭제
product (→ category)         ← 3번째 삭제
category                     ← 4번째 삭제
member                       ← 5번째 삭제
```

`product`를 먼저 삭제하면 `option`이 참조하는 `product`가 없어져서 FK 위반 에러가 발생한다.

#### `@Before`의 실행 시점

```
@Before cleanUp()   ← DB 정리
  시나리오의 조건 step 실행
  시나리오의 만일 step 실행
  시나리오의 그러면 step 실행
@Before cleanUp()   ← 다음 시나리오를 위해 다시 DB 정리
  ...
```

`io.cucumber.java.Before`는 **각 시나리오 실행 직전**에 호출된다.
JUnit의 `@BeforeEach`와 같은 역할이다.

> 주의: `io.cucumber.java.Before`와 JUnit의 `org.junit.jupiter.api.BeforeEach`는 다른 어노테이션이다.

#### 기존 `ApiTest.setUp()`과의 비교

기존 코드:
```java
// ApiTest.java
@BeforeEach
void setUp() {
    RestAssured.port = port;
    wishRepository.deleteAllInBatch();
    optionRepository.deleteAllInBatch();
    productRepository.deleteAllInBatch();
    categoryRepository.deleteAllInBatch();
    memberRepository.deleteAllInBatch();
}
```

Cucumber에서는:
- DB 정리 → `DatabaseCleanupHook.cleanUp()`
- RestAssured port 설정 → 각 Step 클래스에서 `@LocalServerPort`로 직접 주입

---

### 6단계: Step Definitions

Step Definition은 **feature 파일의 한글 문장과 Java 메서드를 연결**하는 코드다.

#### 매칭 원리

```gherkin
만일 "교환권" 이름으로 카테고리를 생성하면
```

이 문장은 아래 Java 메서드와 매칭된다:

```java
@만일("{string} 이름으로 카테고리를 생성하면")
public void 이름으로_카테고리를_생성하면(String name) {
    // name = "교환권"
}
```

- `{string}` — 큰따옴표로 감싸진 문자열을 파라미터로 추출한다. `"교환권"` → `name`
- `{int}` — 숫자를 추출한다. `200` → `statusCode`
- 어노테이션의 문자열 패턴과 feature 파일의 문장이 **정확히 일치**해야 매칭된다.

#### `CommonSteps` — 공통 검증

```java
@그러면("응답 코드는 {int}이다")
public void 응답_코드는_이다(int statusCode) {
    testContext.getLastResponse().then().statusCode(statusCode);
}

@그리고("목록에 {int}개의 항목이 있다")
public void 목록에_n개의_항목이_있다(int count) {
    testContext.getLastResponse().then().body("$", hasSize(count));
}
```

"응답 코드는 200이다", "목록에 2개의 항목이 있다"는 카테고리/상품/선물 시나리오에서 **모두 재사용**된다.
공통 검증 로직을 `CommonSteps`로 모아서 중복을 제거했다.

#### `CategorySteps` — 카테고리 도메인

```java
@조건("{string} 카테고리가 존재한다")
public void 카테고리가_존재한다(String name) {
    Category category = categoryRepository.save(new Category(name));
    testContext.setLastCategory(category);
}
```

- Repository로 직접 DB에 시드 데이터를 넣는다. API 호출 대신 Repository를 사용하는 이유는
  **테스트 데이터 준비를 다른 API의 동작에 의존하지 않기 위해서**다.
- 생성한 카테고리를 `TestContext`에 저장한다. 이후 `ProductSteps`에서 `testContext.getLastCategory()`로 꺼내 쓴다.

```java
@만일("{string} 이름으로 카테고리를 생성하면")
public void 이름으로_카테고리를_생성하면(String name) {
    String body = String.format("""
            {"name": "%s"}
            """, name);
    Response response = RestAssured.given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/categories");
    testContext.setLastResponse(response);
}
```

- "만일" step은 실제 API를 호출하는 **행위(Action)** 를 담당한다.
- 응답을 `TestContext`에 저장하면, 이후 `CommonSteps`의 "그러면" step에서 꺼내 검증한다.
- 컨트롤러가 `@RequestBody`를 사용하므로 JSON body로 전송한다.

#### `ProductSteps` — DataTable 활용

```java
@그리고("{string} 카테고리에 상품이 등록되어 있다")
public void 카테고리에_상품이_등록되어_있다(String categoryName, DataTable dataTable) {
    List<Map<String, String>> rows = dataTable.asMaps();
    Product product = null;
    for (Map<String, String> row : rows) {
        product = productRepository.save(new Product(
                row.get("name"),
                Integer.parseInt(row.get("price")),
                row.get("imageUrl"),
                testContext.getLastCategory()
        ));
    }
    testContext.setLastProduct(product);
}
```

Feature 파일의 DataTable:

```gherkin
그리고 "교환권" 카테고리에 상품이 등록되어 있다
  | name              | price | imageUrl                      |
  | 스타벅스 아메리카노 | 4500  | https://example.com/a.png     |
  | 투썸 케이크         | 15000 | https://example.com/b.png     |
```

- `dataTable.asMaps()` — 표의 첫 행을 key로, 나머지 행을 value로 변환한다.
- 결과: `[{name=스타벅스 아메리카노, price=4500, imageUrl=...}, {name=투썸 케이크, price=15000, imageUrl=...}]`
- 여러 상품을 한 step에서 간결하게 생성할 수 있다.

#### `GiftSteps` — 복합 시나리오

```java
@만일("{string}이 {string}에게 {int}개를 선물하면")
public void 이_에게_n개를_선물하면(String senderName, String receiverName, int quantity) {
    Member sender = testContext.getMember(senderName);
    Member receiver = testContext.getMember(receiverName);
    Option option = testContext.getLastOption();

    String body = String.format("""
            {
                "optionId": %d,
                "quantity": %d,
                "receiverId": %d,
                "message": "생일 축하해!"
            }
            """, option.getId(), quantity, receiver.getId());

    Response response = RestAssured.given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(body)
            .when()
            .post("/api/gifts");
    testContext.setLastResponse(response);
}
```

- 이전 step들에서 `TestContext`에 저장해둔 데이터(Member, Option)를 꺼내 사용한다.
- `{string}` 파라미터가 2개, `{int}` 파라미터가 1개 — Cucumber가 순서대로 매칭한다.

---

### 7단계: 테스트 러너 (`RunCucumberTest.java`)

```java
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.cucumber")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "classpath:features")
public class RunCucumberTest {
}
```

| 어노테이션 | 역할 |
|-----------|------|
| `@Suite` | JUnit Platform Suite로 실행한다 |
| `@IncludeEngines("cucumber")` | Cucumber 엔진만 사용한다 |
| `GLUE_PROPERTY_NAME = "gift.cucumber"` | Step Definition을 찾을 패키지 경로 |
| `FEATURES_PROPERTY_NAME = "classpath:features"` | Feature 파일을 찾을 경로 |

`glue`는 Cucumber에서 **Java 코드(Step, Hook, Config)를 탐색할 패키지**를 의미한다.
`gift.cucumber` 패키지 하위의 모든 클래스를 스캔해서 `@조건`, `@만일`, `@그러면`, `@Before` 등이 붙은 메서드를 찾는다.

---

## 시나리오 실행 흐름 예시

"선물하기 성공" 시나리오의 실제 실행 순서:

```
1. RunCucumberTest 시작
2. Cucumber 엔진이 gift.feature 읽기
3. CucumberSpringConfig 발견 → Spring Boot 앱 시작 (랜덤 포트)
4. "선물하기 성공" 시나리오 시작

5. DatabaseCleanupHook.cleanUp()          ← @Before hook
   → DB 전체 삭제 (wish → option → product → category → member)

6. [배경] 조건 "교환권" 카테고리가 존재한다
   → CategorySteps.카테고리가_존재한다("교환권")
   → categoryRepository.save(new Category("교환권"))
   → TestContext에 category 저장

7. [배경] 그리고 "교환권" 카테고리에 상품이 등록되어 있다
   → ProductSteps.카테고리에_상품이_등록되어_있다("교환권", DataTable)
   → productRepository.save(new Product(..., testContext.getLastCategory()))
   → TestContext에 product 저장

8. [배경] 그리고 상품에 재고 100개의 "기본 옵션"이 등록되어 있다
   → GiftSteps.상품에_재고_n개의_옵션이_등록되어_있다(100, "기본 옵션")
   → optionRepository.save(new Option("기본 옵션", 100, testContext.getLastProduct()))
   → TestContext에 option 저장

9. [배경] 그리고 "보내는사람" 회원이 존재한다
   → GiftSteps.회원이_존재한다("보내는사람")
   → memberRepository.save(new Member("보내는사람", "보내는사람@test.com"))
   → TestContext.members에 저장

10. [배경] 그리고 "받는사람" 회원이 존재한다
    → 위와 동일

11. [시나리오] 만일 "보내는사람"이 "받는사람"에게 3개를 선물하면
    → GiftSteps.이_에게_n개를_선물하면("보내는사람", "받는사람", 3)
    → POST /api/gifts (JSON body)
    → TestContext에 response 저장

12. [시나리오] 그러면 응답 코드는 200이다
    → CommonSteps.응답_코드는_이다(200)
    → testContext.getLastResponse().then().statusCode(200)
    → 통과!
```

---

## 데이터 격리 전략

시나리오 간 데이터 독립성을 보장하는 두 가지 장치:

### 1. DB 레벨 — `DatabaseCleanupHook`

매 시나리오 실행 전 모든 테이블을 비운다.
시나리오 A에서 만든 카테고리가 시나리오 B에 남아있지 않도록 보장한다.

### 2. 객체 레벨 — `@ScenarioScope`

`TestContext`가 시나리오마다 새로 생성된다.
시나리오 A에서 저장한 `lastResponse`가 시나리오 B에서 참조되지 않도록 보장한다.

```
시나리오 A: [DB 정리] → [새 TestContext] → step 실행 → 종료
시나리오 B: [DB 정리] → [새 TestContext] → step 실행 → 종료
                                  ↑
                    완전히 독립된 상태에서 시작
```

---

## 최종 테스트 결과

```bash
./gradlew test

# Cucumber 시나리오 6개:
#   - 카테고리 생성
#   - 카테고리 목록 조회
#   - 상품 목록 조회 시 카테고리와 함께 반환된다
#   - 여러 상품을 조회하면 모두 나타난다
#   - 선물하기 성공
#   - 재고 부족 시 선물하기 실패
#
# RestAssured 테스트 6개 (기존 유지):
#   - CategoryApiTest (2개)
#   - ProductApiTest (2개)
#   - GiftApiTest (2개)
#
# 단위 테스트 2개:
#   - OptionTest.decrease_sufficientStock_reducesQuantity
#   - OptionTest.decrease_insufficientStock_throwsException
#
# 총 14개 테스트 통과
```

---

## 의사결정 기록

| 결정 | 이유 |
|------|------|
| Step 클래스를 도메인별로 분리 | 한 파일이 너무 커지는 것을 방지하고, 관심사를 분리한다 |
| 공통 검증을 `CommonSteps`로 분리 | "응답 코드는 200이다"는 모든 시나리오에서 재사용된다 |
| `TestContext`에 `@ScenarioScope` 적용 | 시나리오 간 상태 격리를 보장한다 |
| 조건(Given) step에서 Repository로 시드 | API 호출로 시드하면 해당 API 버그가 다른 테스트를 깨뜨린다 |
| `@Before` hook으로 DB 정리 | 시나리오 시작 전 깨끗한 상태를 보장한다 |
| 카테고리 생성에 JSON body 사용 | 컨트롤러가 `@RequestBody`를 사용하므로 `formParam` 대신 JSON으로 전송 |
| 기존 RestAssured 테스트 유지 | 두 방식을 공존시켜 비교 가능하게 한다 |
