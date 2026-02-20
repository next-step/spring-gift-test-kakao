---
name: acceptance-test-writer
description: >
  RestAssured 기반 E2E 인수 테스트를 작성한다.
  TEST_STRATEGY.md와 CLAUDE.md의 시나리오 매트릭스를 기반으로
  성공/실패 케이스를 given-when-then 구조로 생성한다.
  대상 API를 인자로 받거나, 미지정 시 전체 API를 순서대로 진행한다.
argument-hint: "[API endpoint, e.g. POST /api/categories]"
allowed-tools: Read, Grep, Glob, Edit, Write, Bash
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
3. 대상 컨트롤러 · 서비스 · 엔티티 코드 읽기
4. 테스트 클래스 생성 (또는 기존 파일에 추가)
5. 성공 시나리오 작성 → ./gradlew test 실행
6. 실패 시 버그 수정 (Red → Green)
7. 실패 시나리오 작성 → ./gradlew test 실행
8. 중복 발견 시 헬퍼 추출 (Refactor)
9. 최종 ./gradlew test 전체 통과 확인
```

## 금지 사항
- 프로덕션 코드 변경
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
