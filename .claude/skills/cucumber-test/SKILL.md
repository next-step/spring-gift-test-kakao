---
name: cucumber-test
description: Cucumber BDD 인수 테스트를 생성한다. 도메인명을 인자로 받아 Feature 파일과 StepDefinitions를 생성하고 기존 테스트를 전환한다.
---

# Cucumber 인수 테스트 생성

$ARGUMENTS 도메인에 대한 Cucumber BDD 인수 테스트를 생성한다.

## 실행 절차

1. **대상 도메인의 소스 코드를 분석한다.**
   - Controller, Service, Entity를 읽어 API 스펙과 비즈니스 로직을 파악한다.
2. **기존 테스트가 있으면 분석한다.**
   - 기존 테스트 시나리오와 검증 항목을 파악한다.
3. **Feature 파일을 생성한다.** (`src/test/resources/features/{도메인}.feature`)
4. **StepDefinitions 클래스를 생성한다.** (`src/test/java/gift/acceptance/{도메인}/{도메인}StepDefinitions.java`)
5. **CommonStepDefinitions에 새로운 공통 스텝이 필요하면 추가한다.**
6. **기존 테스트 파일이 있으면 삭제한다.**
7. **`./gradlew test`로 전체 테스트 통과를 확인한다.**

## Feature 파일 패턴

```gherkin
# language: ko
기능: 카테고리 관리

  시나리오: 유효한 이름이면 카테고리가 생성된다
    만약 이름이 "음료"인 카테고리 생성을 요청하면
    그러면 응답 상태 코드는 200이다
    그리고 응답의 "id" 필드는 비어있지 않다
    그리고 응답의 "name" 필드는 "음료"이다

  시나리오: 카테고리를 생성하면 조회 목록에 포함된다
    조건 이름이 "간식"인 카테고리가 등록되어 있다
    만약 카테고리 목록을 조회하면
    그러면 응답 상태 코드는 200이다
    그리고 응답 목록의 "name" 필드에 "간식"이 포함되어 있다
```

## StepDefinitions 패턴

```java
public class CategoryStepDefinitions {

    @Autowired
    private AcceptanceContext context;

    // 조건(Given): 데이터 준비 — 응답을 context에 저장하지 않는다
    @조건("이름이 {string}인 카테고리가 등록되어 있다")
    public void 이름이_인_카테고리가_등록되어_있다(String name) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200);
    }

    // 만약(When): API 요청 — 응답을 context에 저장한다
    @만약("이름이 {string}인 카테고리 생성을 요청하면")
    public void 이름이_인_카테고리_생성을_요청하면(String name) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .extract();

        context.setResponse(response);
    }
}
```

## 재사용 가능한 공통 스텝 (CommonStepDefinitions)

Then/And 검증은 직접 구현하지 않고 아래 공통 스텝을 재사용한다.

- `응답 상태 코드는 {int}이다`
- `응답의 {string} 필드는 비어있지 않다`
- `응답의 {string} 필드는 {string}이다`
- `응답 목록의 {string} 필드에 {string}이 포함되어 있다`
- `응답은 빈 목록이다`

부족한 공통 스텝이 있으면 `CommonStepDefinitions.java`에 추가한다.