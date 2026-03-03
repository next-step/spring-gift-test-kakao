---
name: write-cucumber-scenario
description: Cucumber BDD 시나리오 하나를 작성할 때 사용한다. Feature 파일에 Gherkin 시나리오를 추가하고 Step Definitions를 구현한다.
argument-hint: 검증할 행위 (예: "위시리스트 추가 시 조회에 포함된다")
---

$ARGUMENTS에 대한 Cucumber BDD 시나리오를 작성한다. 아래 단계를 순서대로 수행한다.

## 1단계: 시나리오 설계

- 검증할 외부 행위를 한 문장으로 정의한다
- 대상 엔드포인트(HTTP method + path)를 확인한다
- 성공/실패 시나리오를 구분한다
- Given(사전 조건) / When(행위) / Then(검증)을 분리한다
- **하나의 시나리오는 하나의 행위만 검증한다**

## 2단계: Feature 파일 작성

`src/test/resources/features/` 에 `.feature` 파일을 작성하거나 기존 파일에 시나리오를 추가한다.

한글 Gherkin 키워드:
- `기능` (Feature)
- `배경` (Background)
- `시나리오` (Scenario)
- `조건` / `먼저` (Given)
- `만약` / `만일` (When)
- `그러면` (Then)
- `그리고` (And)

```gherkin
# language: ko
기능: 선물 전송

  배경:
    조건 회원 "보내는사람"과 "받는사람"이 존재한다
    그리고 "떡볶이" 상품에 재고 10개인 "기본" 옵션이 존재한다

  시나리오: 선물을 보내면 옵션 재고가 감소한다
    만약 "보내는사람"이 "기본" 옵션으로 3개를 선물한다
    그러면 응답 상태 코드는 200이다
    그리고 "기본" 옵션의 재고는 7개이다
```

### 시나리오 작성 원칙
- 시나리오 제목은 비개발자도 이해할 수 있는 비즈니스 언어로 작성한다
- Given에는 API 호출이 아닌 데이터 상태를 서술한다
- When에는 사용자의 행위를 서술한다
- Then에는 기대 결과를 서술한다
- 구현 세부사항(JSON 필드명, 엔드포인트 경로 등)은 Step Definition에 숨긴다

## 3단계: Step Definitions 구현

`src/test/java/gift/acceptance/steps/` 에 도메인별 Step Definition 클래스를 작성한다.

```java
public class GiftStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @만약("{string}이 {string} 옵션으로 {int}개를 선물한다")
    public void 선물을_보낸다(String sender, String optionName, int quantity) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", findMemberId(sender))
                .body(Map.of(...))
                .when()
                .post("/api/gifts");
        sharedContext.setResponse(response);
    }
}
```

### Step Definition 작성 원칙
- SharedContext를 주입받아 시나리오 내 상태를 공유한다
- 파라미터는 Cucumber Expressions(`{string}`, `{int}`)로 추출한다
- 도메인별로 클래스를 분리한다 (GiftStepDefinitions, ProductStepDefinitions 등)
- 공통 Step(응답 검증 등)은 CommonStepDefinitions에 작성한다

## 4단계: 데이터 준비 Step

Given 단계에서 테스트 데이터를 준비한다.

방법 1 — Repository 직접 사용:
```java
@조건("회원 {string}과 {string}이 존재한다")
public void 회원이_존재한다(String name1, String name2) {
    memberRepository.save(new Member(name1, name1 + "@test.com"));
    memberRepository.save(new Member(name2, name2 + "@test.com"));
}
```

방법 2 — JdbcTemplate 사용:
```java
@조건("회원 {string}과 {string}이 존재한다")
public void 회원이_존재한다(String name1, String name2) {
    jdbcTemplate.update("INSERT INTO member ...", name1, ...);
}
```

데이터 정리는 `@Before` hook에서 수행한다.

## 5단계: 검증

```bash
./gradlew test
```

- 새 시나리오가 실행되고 통과하는지 확인한다
- 기존 시나리오가 깨지지 않았는지 확인한다
- 기존 Step Definition을 재사용할 수 있는지 확인한다

## 주의사항

- Step Definition 메서드는 전역적이다 — 동일한 패턴의 Step이 두 곳에 정의되면 충돌한다
- `@ScenarioScope` Bean을 활용하여 시나리오 간 격리를 보장한다
- 시나리오 아웃라인(`시나리오 개요` + `예시`)으로 파라미터화된 테스트를 작성할 수 있다
