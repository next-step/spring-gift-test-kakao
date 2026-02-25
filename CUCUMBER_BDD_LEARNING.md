# Cucumber BDD 학습 기록

## 1. Cucumber BDD란?

Cucumber는 **Behavior-Driven Development(BDD)** 프레임워크로,
비즈니스 언어(자연어)로 작성된 테스트 시나리오를 실행 가능한 코드와 연결한다.

핵심 가치: **비개발자(기획자, QA)도 테스트 시나리오를 읽고 이해할 수 있다.**

### 기존 RestAssured 테스트 vs Cucumber BDD

| 항목 | RestAssured 테스트 | Cucumber BDD |
|---|---|---|
| 시나리오 표현 | Java 코드 안에 숨어있음 | `.feature` 파일에 자연어로 드러남 |
| 비개발자 가독성 | 낮음 | 높음 |
| 재사용성 | 메서드 단위 | Step 단위 (여러 시나리오에서 재사용) |
| 구조 | 테스트 클래스 = 시나리오 | Feature 파일(시나리오) + Step Definition(구현) 분리 |

---

## 2. 핵심 구성 요소

### 2.1 Feature 파일 (Gherkin 문법)

`src/test/resources/features/*.feature`에 위치하며, Gherkin 문법으로 작성한다.

```gherkin
# language: ko
기능: 선물하기

  시나리오: 재고가 충분할 때 선물하기를 하면 옵션 수량이 감소한다
    조건 보내는 회원과 받는 회원이 존재한다
    그리고 카테고리 "테스트카테고리"와 상품 "테스트상품"과 수량이 10인 옵션 "테스트옵션"이 존재한다
    만약 회원 1이 옵션 1을 수량 3으로 회원 2에게 선물한다
    그러면 응답 상태코드는 200이다
    그리고 옵션 1의 수량은 7이다
```

**한글 키워드 매핑:**

| 영문 | 한글 | 역할 |
|---|---|---|
| Feature | 기능 | 테스트 대상 기능 이름 |
| Scenario | 시나리오 | 하나의 테스트 케이스 |
| Given | 조건 | 사전 조건 준비 |
| When | 만약 | 사용자 행동 (API 호출 등) |
| Then | 그러면 | 결과 검증 |
| And | 그리고 | 앞 키워드 이어서 사용 |

`# language: ko`를 파일 상단에 선언하면 한글 키워드를 사용할 수 있다.
이를 선언하지 않으면 영문 키워드(Given/When/Then)만 인식된다.

### 2.2 Step Definitions

Feature 파일의 각 줄(스텝)을 실제 Java 코드로 구현한 것이다.

```java
@만약("회원 {long}이 옵션 {long}을 수량 {int}으로 회원 {long}에게 선물한다")
public void 회원이_옵션을_수량으로_회원에게_선물한다(long senderId, long optionId, int quantity, long receiverId) {
    var response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", senderId)
            .body(Map.of("optionId", optionId, "quantity", quantity, "receiverId", receiverId, "message", "선물입니다"))
            .post("/api/gifts");
    sharedContext.setResponse(response);
}
```

- `{int}`, `{long}`, `{string}` — Cucumber 표현식으로, Feature 파일의 값을 파라미터로 추출
- 도메인 모델의 타입과 일치시켜야 한다 (ID는 `{long}`, 수량/가격은 `{int}`)
- 한글 어노테이션(`@만약`, `@그러면`, `@그리고`)은 `io.cucumber.java.ko` 패키지에서 제공
- 영문 어노테이션(`@Given`, `@When`, `@Then`)은 `io.cucumber.java.en` 패키지에서 제공

### 2.3 Spring Boot 통합

**CucumberSpringConfiguration** — Cucumber가 Spring Boot 컨텍스트를 사용하도록 연결:

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {
}
```

- `@CucumberContextConfiguration` — 이 클래스가 Cucumber의 Spring 설정임을 선언
- `@SpringBootTest(RANDOM_PORT)` — 내장 톰캣을 랜덤 포트로 기동
- 이 설정이 있어야 Step Definition에서 `@Autowired`, `@LocalServerPort` 등 Spring 기능 사용 가능

### 2.4 실행 진입점 (Suite)

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.cucumber")
public class CucumberTest {
}
```

- `@Suite` — JUnit Platform Suite로 실행
- `@IncludeEngines("cucumber")` — Cucumber 엔진 사용
- `@SelectClasspathResource("features")` — `src/test/resources/features/` 의 `.feature` 파일을 읽음
- `GLUE` — Step Definition을 찾을 패키지 위치 지정

---

## 3. 시나리오 간 데이터 공유와 격리

### 3.1 Response 공유 — @ScenarioScope

하나의 시나리오 안에서 When 스텝의 응답을 Then 스텝에서 검증해야 한다.
이를 위해 `@ScenarioScope` 빈을 사용한다:

```java
@Component
@ScenarioScope
public class SharedContext {
    private Response response;
    // getter, setter
}
```

- `@ScenarioScope` — 시나리오마다 새 인스턴스가 생성되고, 시나리오 종료 시 폐기
- 시나리오 A의 Response가 시나리오 B에 영향을 주지 않음

### 3.2 데이터 격리 — @Before hook

```java
@Before
public void setUp() {
    RestAssured.port = port;
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
    jdbcTemplate.execute("TRUNCATE TABLE \"OPTION\"");
    // ... 모든 테이블 TRUNCATE
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
}
```

- `@Before` (io.cucumber.java.Before) — 매 시나리오 실행 전 자동 호출
- 모든 테이블을 TRUNCATE하여 이전 시나리오의 데이터가 남지 않도록 보장
- Feature 파일에 "데이터 초기화" 스텝을 명시할 필요 없이 자동으로 격리됨

---

## 4. 의존성 정리

```groovy
// build.gradle
testImplementation 'io.cucumber:cucumber-java:7.22.0'              // 코어 + 어노테이션
testImplementation 'io.cucumber:cucumber-spring:7.22.0'            // Spring Boot 통합
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.0'  // JUnit Platform 엔진
testImplementation 'org.junit.platform:junit-platform-suite:1.11.4'     // @Suite API
```

---

## 5. 프로젝트 파일 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberSpringConfiguration.java  -- Spring Boot 연결 설정
│   │   ├── CucumberTest.java                 -- 실행 진입점 (@Suite)
│   │   ├── SharedContext.java                -- 시나리오 내 Response 공유
│   │   ├── CommonStepDefinitions.java        -- 공통 (데이터 초기화, 상태코드 검증)
│   │   ├── CategoryStepDefinitions.java      -- 카테고리 관련 스텝
│   │   ├── ProductStepDefinitions.java       -- 상품 관련 스텝
│   │   └── GiftStepDefinitions.java          -- 선물하기 관련 스텝
│   ├── BaseBehaviorTest.java                 -- 기존 RestAssured 테스트 베이스
│   ├── CategoryBehaviorTest.java             -- 기존 테스트 (유지)
│   ├── GiftBehaviorTest.java                 -- 기존 테스트 (유지)
│   └── ProductBehaviorTest.java              -- 기존 테스트 (유지)
└── resources/
    ├── features/
    │   ├── category.feature                  -- 카테고리 시나리오 (1개)
    │   ├── product.feature                   -- 상품 시나리오 (2개)
    │   └── gift.feature                      -- 선물하기 시나리오 (4개)
    └── sql/                                  -- 기존 SQL 파일 (유지)
```

---

## 6. 실행 흐름

```
./gradlew test
  └─ JUnit이 CucumberTest 발견
       └─ @Suite → Cucumber 엔진 실행
            └─ features/*.feature 파일 읽기
                 └─ 각 시나리오마다:
                      1. @Before hook → 포트 설정 + DB TRUNCATE
                      2. 조건 스텝 → 데이터 준비 (SQL 또는 API)
                      3. 만약 스텝 → API 호출, Response를 SharedContext에 저장
                      4. 그러면 스텝 → 상태코드/DB 상태 검증
```

---

## 7. 주요 의사결정과 이유

| 결정 | 이유 |
|---|---|
| 한글 Gherkin (`# language: ko`) 사용 | 요구사항에 "한글 Given-When-Then" 명시. 비개발자 가독성 극대화 |
| `@Before` hook으로 데이터 초기화 | Feature 파일에서 반복 제거. 시나리오 작성자가 격리를 신경 쓸 필요 없음 |
| `@ScenarioScope`로 Response 공유 | 시나리오 간 격리 보장. 별도의 ThreadLocal이나 static 변수 불필요 |
| Step Definition을 도메인별로 분리 | 재사용성과 유지보수성. 카테고리/상품/선물 각각 독립적으로 관리 |
| 기존 RestAssured 테스트 유지 | 두 방식의 테스트가 공존하면서 안전망 역할. 점진적 전환 가능 |
| Gift 데이터 준비를 SQL로 수행 | 회원/옵션 등은 생성 API가 없어서 API 경유 불가. 최소 범위의 SQL 사용 |

---

## 8. 미션 수행에 도움 되는 질문과 답변

### 전체 미션

**Q. 다른 개발자도 동일한 명령으로 실행 가능한가?**

가능하다. `./gradlew test` 한 줄로 모든 테스트가 실행된다.
Gradle Wrapper가 포함되어 있어 Gradle 별도 설치가 불필요하고,
H2 인메모리 DB를 사용하므로 외부 DB 설정 없이 바로 실행할 수 있다.

**Q. 실행 방법이 충분히 단순한가?**

단순하다. Java 21만 설치되어 있으면 `./gradlew test`로 끝난다.
별도의 환경 변수 설정, DB 기동, 설정 파일 수정이 필요 없다.

**Q. 실패했을 때 원인을 쉽게 파악할 수 있는가?**

Feature 파일의 시나리오 이름이 한글로 되어 있어 어떤 행위가 실패했는지 바로 파악 가능하다.
예: `시나리오: 재고가 부족할 때 선물하기를 하면 거부되고 재고가 유지된다 FAILED`
Step Definition에서 AssertJ를 사용하므로 기대값과 실제값이 명확하게 출력된다.

### 요구사항 1 (Cucumber)

**Q. 비개발자가 시나리오를 읽고 이해할 수 있는가?**

가능하다. `# language: ko`를 사용하여 Feature 파일이 완전한 한글 문장으로 되어 있다.
```
조건 보내는 회원과 받는 회원이 존재한다
만약 회원 1이 옵션 1을 수량 3으로 회원 2에게 선물한다
그러면 응답 상태코드는 200이다
그리고 옵션 1의 수량은 7이다
```
코드를 모르더라도 "선물하면 수량이 줄어든다"는 행위를 이해할 수 있다.

**Q. Step Definitions는 재사용 가능하게 작성되었는가?**

재사용 가능하다. 예시:
- `응답 상태코드는 {int}이다` — 모든 시나리오에서 공통으로 사용 (CommonStepDefinitions)
- `보내는 회원과 받는 회원이 존재한다` — 선물하기 시나리오 4개 중 3개에서 재사용
- `옵션 {long}의 수량은 {int}이다` — 재고 검증이 필요한 모든 시나리오에서 재사용

Cucumber 표현식의 `{int}`, `{long}`, `{string}` 파라미터를 활용하여
값만 다른 동일한 패턴의 스텝을 하나의 메서드로 처리한다.

**Q. 시나리오 간 데이터가 격리되는가?**

격리된다. 두 가지 메커니즘으로 보장한다:
1. `@Before` hook — 매 시나리오 실행 전 모든 테이블을 TRUNCATE
2. `@ScenarioScope` — SharedContext(Response 객체)가 시나리오마다 새로 생성

시나리오 실행 순서에 의존하지 않으며, 어떤 순서로 실행해도 동일한 결과를 얻는다.

---

## 9. 탐구 질문과 답변

**Q. Gherkin의 Given/When/Then은 무엇을 의미하는가?**

테스트 시나리오를 3단계로 구조화하는 패턴이다:
- **Given (조건)** — 테스트의 사전 조건을 준비한다. 예: 회원, 상품, 옵션 데이터 생성
- **When (만약)** — 사용자의 행동을 실행한다. 예: API 호출
- **Then (그러면)** — 행동의 결과를 검증한다. 예: 응답 상태코드, DB 상태 확인

이 패턴은 Arrange-Act-Assert와 동일한 구조이며,
Cucumber에서는 자연어로 표현하여 비개발자도 이해할 수 있게 한다.

**Q. Step Definitions에서 파라미터를 어떻게 추출하는가?**

Cucumber 표현식을 사용한다:
- `{string}` — 큰따옴표로 감싼 문자열을 `String`으로 추출. 예: `"테스트카테고리"` → `String name`
- `{int}` — 정수를 `int`로 추출. 예: `10000` → `int price`
- `{long}` — 정수를 `long`으로 추출. 예: `9999` → `long categoryId`

도메인 모델의 타입과 일치시키는 것이 중요하다.
ID 필드가 `Long`이면 `{long}`을, 수량이 `int`이면 `{int}`를 사용해야 한다.

```java
// Feature: 만약 회원 1이 옵션 1을 수량 3으로 회원 2에게 선물한다
@만약("회원 {long}이 옵션 {long}을 수량 {int}으로 회원 {long}에게 선물한다")
public void 선물한다(long senderId, long optionId, int quantity, long receiverId) { ... }
```

**Q. 시나리오 간 Response 객체를 어떻게 공유하는가?**

`@ScenarioScope` 빈을 사용한다:
```java
@Component
@ScenarioScope  // 시나리오마다 새 인스턴스 생성
public class SharedContext {
    private Response response;
}
```

When 스텝에서 API를 호출하고 Response를 `SharedContext`에 저장하면,
Then 스텝에서 꺼내서 검증한다. `@ScenarioScope`이므로 시나리오가 끝나면 폐기되어
다음 시나리오에 영향을 주지 않는다.

**Q. @Before hook은 언제 실행되는가?**

`io.cucumber.java.Before`는 **각 시나리오가 시작되기 직전**에 실행된다.
시나리오마다 한 번씩 호출되므로, 7개 시나리오가 있으면 7번 실행된다.

이 프로젝트에서는 `@Before`에서 두 가지를 수행한다:
1. `RestAssured.port = port` — RestAssured가 Spring Boot 서버 포트를 알도록 설정
2. 모든 테이블 TRUNCATE — 이전 시나리오의 데이터 정리

주의: JUnit의 `@BeforeEach`와 다른 패키지(`io.cucumber.java.Before`)이다.

**Q. RestAssured 포트 설정은 어디서 하는가?**

`CommonStepDefinitions`의 `@Before` hook에서 설정한다:
```java
@LocalServerPort
int port;

@Before
public void setUp() {
    RestAssured.port = port;  // Spring Boot가 기동된 랜덤 포트를 RestAssured에 전달
}
```

`@SpringBootTest(RANDOM_PORT)`로 서버가 랜덤 포트에 기동되므로,
`@LocalServerPort`로 실제 포트를 주입받아 RestAssured에 설정해야
API 호출이 올바른 포트로 전달된다.
