---
name: acceptance-test-writer
description: >
  RestAssured 기반 E2E 인수 테스트를 작성한다.
  TEST_STRATEGY.md와 CLAUDE.md의 시나리오 매트릭스를 기반으로
  성공/실패 케이스를 given-when-then 구조로 생성한다.
  대상 API를 인자로 받거나, 미지정 시 전체 API를 순서대로 진행한다.
  이 스킬은 테스트 코드만 작성하며 프로덕션 코드를 절대 수정하지 않는다.
argument-hint: "[API endpoint, e.g. POST /api/categories]"
allowed-tools: Read, Grep, Glob, Bash(./gradlew *)
---

# acceptance-test-writer

RestAssured 기반 E2E 인수 테스트를 작성한다.

## 역할
- `TEST_STRATEGY.md`의 검증 행위 목록 · 데이터 전략 · 검증 계층을 준수한다.
- `CLAUDE.md`의 Test Stack & Rules, E2E Scenario Matrix를 따른다.
- Red-Green-Refactor 사이클로 점진적으로 테스트를 추가한다.

## 입력 계약
- 사용자가 대상 API(예: `POST /api/categories`)를 지정하면 해당 API의 성공/실패 시나리오를 작성한다.
- 대상이 없으면 `CLAUDE.md §3 E2E Scenario Matrix` 전체를 순서대로 진행한다.
- 테스트 작성 전 반드시 대상 컨트롤러 · 서비스 · 엔티티 코드를 읽는다.

## 전제 조건 확인
테스트 작성 전 아래를 확인하고, 누락 시 먼저 조치한다:
1. `build.gradle`에 `testImplementation 'io.rest-assured:rest-assured'` 존재
2. `src/test/resources/application.properties`에 H2 설정 존재
3. 기반 클래스(`AcceptanceTestBase` 등) 존재

## 테스트 클래스 구조

```java
package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// Repository import — 데이터 정리 및 API 없는 엔티티 생성용
import gift.model.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XxxAcceptanceTest {

    @LocalServerPort
    int port;

    // 데이터 정리에 필요한 Repository들을 @Autowired
    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순으로 삭제 — 테스트 격리 보장
        // wishRepository.deleteAll();
        // optionRepository.deleteAll();
        // productRepository.deleteAll();
        // categoryRepository.deleteAll();
        // memberRepository.deleteAll();
    }
}
```

## 테스트 메서드 작성 규칙

### 명명: 한글, `대상_상황_기대결과`
```java
@Test
void 카테고리_생성_성공() { }

@Test
void 상품_생성_실패_존재하지_않는_카테고리() { }
```

### 구조: given-when-then 주석 필수
```java
@Test
void 카테고리_생성_성공() {
    // given
    var request = Map.of("name", "전자기기");

    // when
    var response = given()
        .contentType(ContentType.JSON)
        .body(request)
    .when()
        .post("/api/categories");

    // then
    response.then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("전자기기"));
}
```

## 검증 계층 (Layer 1 → 2 → 3)

모든 테스트에서 아래 순서로 검증한다:

| Layer | 대상 | 필수 여부 | 예시 |
|-------|------|-----------|------|
| 1 | HTTP 상태 코드 | 모든 테스트 | `.statusCode(201)` |
| 2 | 응답 본문 필드/값 | 대부분 | `.body("id", notNullValue())` |
| 3 | 상태 변화 (DB) | 상태 변경 API만 | `assertThat(option.getQuantity()).isEqualTo(...)` |

## 데이터 생성 전략

### API가 있는 엔티티 — API 호출로 생성 (Black-box)
```java
Long createCategory(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", name))
    .when()
        .post("/api/categories")
    .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");
}

Long createProduct(String name, int price, String imageUrl, Long categoryId) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", name,
            "price", price,
            "imageUrl", imageUrl,
            "categoryId", categoryId
        ))
    .when()
        .post("/api/products")
    .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");
}
```

### API가 없는 엔티티 — Repository로 생성 (White-box)
```java
// Member — 생성 API 없음
Member createMember(String name, String email) {
    return memberRepository.save(new Member(name, email));
}

// Option — 생성 API 없음
Option createOption(String name, int quantity, Product product) {
    return optionRepository.save(new Option(name, quantity, product));
}
```

### Product 엔티티 조회 (Option 생성 시 필요)
```java
// Repository로 Product 엔티티를 조회하여 Option에 전달
Product product = productRepository.findById(productId).orElseThrow();
Option option = createOption("기본 옵션", 10, product);
```

헬퍼 추출 후 반드시 `./gradlew test` 전체 통과를 확인한다.

## 실행 흐름

```
1. 대상 API 확인 (사용자 지정 또는 Scenario Matrix 순서)
2. 전제 조건 확인 (build.gradle, application.properties, 기반 클래스)
3. 대상 컨트롤러 · 서비스 · 엔티티 코드 읽기 (Read only)
4. 테스트 클래스 생성 (또는 기존 파일에 추가) — src/test/ 경로만 허용
5. 프로덕션 코드의 현재 동작을 기준으로 테스트 작성
6. ./gradlew test 실행
7. 테스트 실패 시 → 아래 "테스트 실패 대응" 규칙 적용
8. 중복 발견 시 헬퍼 추출 (Refactor) — src/test/ 경로만 허용
9. 최종 ./gradlew test 전체 통과 확인
```

## 테스트 실패 대응

테스트가 실패했을 때 **프로덕션 코드를 수정하지 않는다.** 대신:

1. **테스트 코드 오류인 경우**: 테스트 코드를 수정한다 (src/test/ 내에서만)
2. **프로덕션 버그 발견인 경우**: 아래 절차를 따른다
   - 테스트의 기대값을 프로덕션의 **현재 동작**에 맞춘다
     (예: 201이 아닌 200을 반환하면 `.statusCode(200)`)
   - `@Disabled("BUG: @RequestBody 누락으로 JSON 바인딩 안 됨")` 로 마킹하거나,
     현재 동작 기준으로 테스트를 통과시킨 뒤 주석으로 기대 동작을 남긴다
   - 발견된 버그를 **테스트 파일 상단 주석** 또는 별도 섹션에 기록한다

```java
// 발견된 프로덕션 버그:
// - CategoryRestController.create(): @RequestBody 누락 → JSON 바인딩 안 됨
// - CategoryRestController.create(): @ResponseStatus(CREATED) 없음 → 200 반환

@Test
@Disabled("BUG: @RequestBody 누락으로 name이 null로 저장됨. 프로덕션 수정 후 활성화")
void 카테고리_생성_성공() { ... }
```

## 파일 수정 범위 제약 (CRITICAL)

### 수정 가능 경로
- `src/test/**/*` — 테스트 코드 생성/수정
- `build.gradle` — 테스트 의존성 추가만 허용 (`testImplementation` 행만)
- `src/test/resources/**/*` — 테스트 설정 파일

### 절대 수정 금지 경로
- `src/main/**/*` — 프로덕션 코드 일체
- `src/main/resources/**/*` — 프로덕션 설정 파일

이 규칙은 어떤 상황에서도 예외 없이 적용된다.
프로덕션 버그를 발견해도 테스트 코드에 기록만 하고, 수정은 사용자에게 위임한다.

## 금지 사항
- `src/main/` 하위 파일 생성/수정/삭제 (CRITICAL — 절대 금지)
- 한 번에 여러 API 테스트를 동시 작성 (하나씩 점진적으로)
- 테스트 코드에서 프로덕션 로직 복제
- `@Transactional`로 테스트 격리 시도 (RestAssured는 별도 HTTP 요청)
- Flaky 테스트 방치

## 사용 예시

```text
/acceptance-test-writer
```

```text
/acceptance-test-writer POST /api/categories
```

```text
/acceptance-test-writer Gift API 전체
```
