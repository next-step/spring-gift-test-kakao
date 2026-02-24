# 요구사항 1: Cucumber BDD 적용

## Gherkin의 Given/When/Then은 무엇을 의미하는가?

테스트 시나리오를 3단계로 나눈 구조다.

- **Given (조건)**: 사전 조건 준비. "카테고리가 등록되어 있다"
- **When (만약)**: 테스트 대상 행위. "상품 생성을 요청하면"
- **Then (그러면)**: 기대 결과 검증. "응답 코드가 201이다"

```gherkin
# language: ko
시나리오: 유효한 데이터이면 상품이 생성된다
조건 이름이 "음료"인 카테고리가 등록되어 있다
만약 이름이 "아메리카노"이고 가격이 4500인 상품 생성을 요청하면
그러면 응답 코드가 201이다
```

비개발자도 읽을 수 있다는 게 핵심이다.

## Step Definitions에서 파라미터를 어떻게 추출하는가?

Gherkin 스텝의 `{string}`, `{int}` 등이 메서드 파라미터로 매핑된다.

```java

@만약("이름이 {string}이고 가격이 {int}인 상품 생성을 요청하면")
public void 상품_생성을_요청하면(String name, int price) {
    // name = "아메리카노", price = 4500
}
```

Cucumber가 정규식으로 텍스트를 파싱해서 타입에 맞게 변환해준다.

## 시나리오 간 Response 객체를 어떻게 공유하는가?

`@ScenarioScope` Bean을 사용한다.

```java

@Component
@ScenarioScope
public class AcceptanceContext {
    private ExtractableResponse<Response> response;
    private Long categoryId;
    // getter, setter
}
```

- `@ScenarioScope`: 시나리오마다 새 인스턴스가 생성된다
- Given에서 ID를 저장하고, When에서 응답을 저장하고, Then에서 읽어서 검증한다
- 시나리오가 끝나면 자동으로 폐기되므로 시나리오 간 데이터가 격리된다

## @Before hook은 언제 실행되는가?

각 시나리오 실행 직전에 실행된다.

```java

@Before
public void setUp() {
    RestAssured.port = port;
    databaseCleanup.execute();  // DB 초기화
}
```

시나리오마다 DB를 초기화하므로, 이전 시나리오의 데이터가 다음 시나리오에 영향을 주지 않는다.

## RestAssured 포트 설정은 어디서 하는가?

`@SpringBootTest(RANDOM_PORT)`로 띄우면 `@LocalServerPort`로 포트를 받아온다.

```java

@LocalServerPort
private int port;

@Before
public void setUp() {
    RestAssured.port = port;
}
```

이후 `RestAssured.given().body(...).post("/api/products")` 처럼 호출하면 자동으로 해당 포트로 요청이 간다.

## 한국어 조사 문제

Cucumber는 스텝 텍스트를 정확히 매칭한다. 한국어 조사가 숫자에 따라 "로/으로"로 바뀌면 매칭이 실패한다.

```gherkin
만약 수량 3으로 선물 전달을 요청하면    # OK
만약 수량 1로 선물 전달을 요청하면      # FAIL — "로" ≠ "으로"
```

조사를 하나로 통일하거나, 조사가 변하지 않는 표현으로 작성해야 한다.

## 공통 스텝과 도메인 스텝을 왜 분리하는가?

```
CommonStepDefinitions  — 응답 코드 검증, 필드 검증, 빈 목록 (도메인 무관)
CategoryStepDefinitions — 카테고리 Given/When
ProductStepDefinitions  — 상품 Given/When
GiftStepDefinitions     — 선물 Given/When
```

- Then(검증)은 도메인과 무관하게 동일한 패턴이므로 공통으로 뺀다
- Given/When은 도메인별 API가 다르므로 분리한다
- Cucumber는 glue 패키지 전체에서 스텝을 탐색하므로, 카테고리 Given을 상품 시나리오에서 그대로 재사용할 수 있다
