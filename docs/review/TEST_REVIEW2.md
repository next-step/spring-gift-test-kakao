# Test Code Review Report (2차)

## 총평

이전 리뷰 대비 대폭 개선되었습니다. 중간 단계 응답 검증, SQL 스크립트 조합 분리, 상수화, 내용 검증 강화, IDENTITY 리셋 등 핵심 피드백이 반영되어 **테스트의 신뢰성과 유지보수성이 크게 향상**되었습니다. 남은 문제는 코드 중복 최소화와 엣지 케이스 커버리지 확장입니다.

---

## 이전 리뷰 대비 개선 사항

| 이전 지적 | 현재 상태 |
|-----------|-----------|
| 🔴 중간 단계 응답 미검증 | ✅ `.then().statusCode(200)` 추가 |
| 🟡 CLAUDE.md와 RestAssured 불일치 | ✅ CLAUDE.md를 `RestAssured`로 갱신 |
| 🟡 SQL 스크립트 데이터 중복 | ✅ 엔티티별 분리, 조합 방식으로 변경 |
| 🟡 하드코딩 ID | ✅ `SENDER_ID`, `TALL_OPTION_ID` 등 상수화 |
| 🟡 카테고리 여러 개 테스트 내용 미검증 | ✅ `hasItems("음료", "디저트", "케이크")` 추가 |
| 🟢 `cleanup.sql` IDENTITY 미리셋 | ✅ `ALTER TABLE ... RESTART WITH 1` 추가 |
| 🟢 `AcceptanceTest.port` 접근 제어자 | ✅ `protected`로 변경 |

---

## 발견된 문제점

### 🟡 경고 #1 — `GiftAcceptanceTest`에 동일한 `@Sql` 어노테이션 4회 반복

**파일:** `GiftAcceptanceTest.java:20, 31, 39, 47`

```java
@Test
@Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql",
                "/sql/option-data.sql", "/sql/member-data.sql"},
     executionPhase = BEFORE_TEST_METHOD)
void 선물을_보내면_재고가_차감된다() { ... }

@Test
@Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql",
                "/sql/option-data.sql", "/sql/member-data.sql"},
     executionPhase = BEFORE_TEST_METHOD)
void 재고보다_많은_수량으로_선물을_보내면_실패한다() { ... }

// ... 나머지 2개도 동일
```

4개 테스트 메서드 모두 **완전히 동일한** `@Sql` 어노테이션을 갖고 있습니다. `AcceptanceTest`의 `@SqlMergeMode(MERGE)` 덕분에 클래스 레벨 `@Sql`이 `cleanup.sql`과 병합됩니다. 클래스 레벨로 올리면 중복을 제거하고, 새 테스트 추가 시 `@Sql` 누락 실수를 방지할 수 있습니다.

**개선된 코드:**

```java
@Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql",
                "/sql/option-data.sql", "/sql/member-data.sql"},
     executionPhase = BEFORE_TEST_METHOD)
class GiftAcceptanceTest extends AcceptanceTest {

    @Test
    void 선물을_보내면_재고가_차감된다() { ... }

    @Test
    void 재고보다_많은_수량으로_선물을_보내면_실패한다() { ... }

    // @Sql 없이도 클래스 레벨 + cleanup.sql이 MERGE됨
}
```

---

### 🟡 경고 #2 — 재고 차감의 간접 검증만 존재

**파일:** `GiftAcceptanceTest.java:21-28`

```java
void 선물을_보내면_재고가_차감된다() {
    선물_보내기(SENDER_ID, TALL_OPTION_ID, 7, RECEIVER_ID).then().statusCode(200);

    // 잔여 재고를 "다음 요청의 실패"로 간접 검증
    선물_보내기(SENDER_ID, TALL_OPTION_ID, 5, RECEIVER_ID).then().statusCode(500);
}
```

재고가 실제로 3개 남았는지를 직접 확인하지 않고, "5개 요청이 실패한다"는 사실로 간접 추론합니다. 이 방식은 `Option.decrease()`의 조건이 `<` 대신 `<=`로 바뀌어도 테스트가 통과할 수 있어 **경계값 변경에 취약**합니다.

현재 GET `/api/options` 엔드포인트가 없어 직접 검증이 불가하지만, CLAUDE.md의 "API 응답과 후속 API 호출로 확인" 원칙을 완전히 충족하려면 옵션 조회 API가 필요합니다.

**개선 방안:** 옵션 조회 API 추가 후 직접 검증

```java
void 선물을_보내면_재고가_차감된다() {
    선물_보내기(SENDER_ID, TALL_OPTION_ID, 7, RECEIVER_ID).then().statusCode(200);

    // 잔여 재고를 직접 검증
    given()
    .when()
            .get("/api/options/{id}", TALL_OPTION_ID)
    .then()
            .statusCode(200)
            .body("quantity", equalTo(3));
}
```

---

### 🟡 경고 #3 — `CategoryAcceptanceTest`에서 테스트 픽스처 상수 미적용

**파일:** `CategoryAcceptanceTest.java:15, 35-37`

```java
given().queryParam("name", "음료").when().post("/api/categories").then().statusCode(200);
given().queryParam("name", "디저트").when().post("/api/categories").then().statusCode(200);
given().queryParam("name", "케이크").when().post("/api/categories").then().statusCode(200);
```

`ProductAcceptanceTest`와 `GiftAcceptanceTest`는 상수(`CATEGORY_ID`, `SENDER_ID` 등)를 사용하는데, `CategoryAcceptanceTest`는 문자열 리터럴이 직접 사용되어 일관성이 떨어집니다. Category 테스트는 API로 데이터를 생성하므로 SQL ID 결합 문제는 없지만, `"음료"` 같은 값이 When과 Then 양쪽에 흩어져 있어 변경 시 양쪽을 모두 수정해야 합니다.

**개선된 코드:**

```java
class CategoryAcceptanceTest extends AcceptanceTest {

    private static final String CATEGORY_BEVERAGE = "음료";
    private static final String CATEGORY_DESSERT = "디저트";
    private static final String CATEGORY_CAKE = "케이크";

    @Test
    void 카테고리를_생성하면_목록에서_조회된다() {
        given().queryParam("name", CATEGORY_BEVERAGE).when()
                .post("/api/categories").then().statusCode(200);

        given().when().get("/api/categories").then()
                .statusCode(200)
                .body(".", hasSize(1))
                .body("[0].name", equalTo(CATEGORY_BEVERAGE));
    }
}
```

---

### 🟢 개선 권장 #1 — 누락된 엣지 케이스 테스트

프로덕션 코드의 취약점(`Option.decrease()`의 음수/0 가드 미존재)을 드러낼 수 있는 테스트가 부재합니다.

| 시나리오 | 검증 대상 | 기대 결과 |
|----------|-----------|-----------|
| 수량 0으로 선물 | `Option.decrease(0)` → no-op | 실패해야 함 (현재는 성공) |
| 음수 수량으로 선물 | `Option.decrease(-5)` → 재고 증가 | 실패해야 함 (현재는 **재고가 증가**) |
| 자기 자신에게 선물 | `senderId == receiverId` | 비즈니스 규칙에 따라 결정 |
| 존재하지 않는 발신자 | `Member-Id: 999` | 실패해야 함 |

**특히 음수 수량 테스트는 보안 취약점을 검증하는 핵심 케이스입니다:**

```java
@Test
void 음수_수량으로_선물을_보내면_실패한다() {
    선물_보내기(SENDER_ID, TALL_OPTION_ID, -5, RECEIVER_ID).then()
            .statusCode(400); // 현재 코드에서는 성공하며 재고가 증가하는 버그 존재
}

@Test
void 수량_0으로_선물을_보내면_실패한다() {
    선물_보내기(SENDER_ID, TALL_OPTION_ID, 0, RECEIVER_ID).then()
            .statusCode(400); // 현재 코드에서는 no-op으로 성공
}
```

---

### 🟢 개선 권장 #2 — `ProductAcceptanceTest`에서 가격/이미지 검증 누락

**파일:** `ProductAcceptanceTest.java:30-37`

```java
given().when().get("/api/products").then()
        .statusCode(200)
        .body(".", hasSize(1))
        .body("[0].name", equalTo("아메리카노"));
        // price, imageUrl, category 검증 없음
```

상품 이름만 검증하고, 가격(`4500`), 이미지 URL, 소속 카테고리 등 나머지 필드를 검증하지 않습니다. 만약 `price` 바인딩에 문제가 있어 `0`으로 저장되더라도 테스트는 통과합니다.

**개선된 코드:**

```java
given().when().get("/api/products").then()
        .statusCode(200)
        .body(".", hasSize(1))
        .body("[0].name", equalTo("아메리카노"))
        .body("[0].price", equalTo(4500))
        .body("[0].imageUrl", equalTo("https://example.com/img.jpg"));
```

---

### 🟢 개선 권장 #3 — Wish, Option 도메인 테스트 부재

CLAUDE.md 클래스 네이밍 규칙: `{도메인}AcceptanceTest`

현재 존재하는 테스트: `CategoryAcceptanceTest`, `ProductAcceptanceTest`, `GiftAcceptanceTest`

누락된 테스트: `WishAcceptanceTest`, `OptionAcceptanceTest`

Wish와 Option은 현재 REST API가 노출되어 있지 않으므로 인수 테스트 대상이 아니지만, 해당 서비스 로직이 존재하는 이상 단위 테스트 또는 통합 테스트 레벨에서의 검증을 고려해볼 수 있습니다.

---

## 추가 조언

1. **`@Sql` 중복은 클래스 레벨로 올리세요.** `@SqlMergeMode(MERGE)`의 목적이 바로 이것입니다. 메서드별 `@Sql`은 해당 테스트만의 **추가 데이터가 필요한 경우**에 사용하는 것이 MERGE 모드의 의도에 부합합니다.

2. **테스트가 프로덕션 버그를 드러내도록 작성하세요.** 현재 테스트는 "정상 동작"과 "초과 시 실패"를 잘 검증하지만, `Option.decrease()`의 음수 수량 취약점은 테스트가 있었다면 즉시 발견되었을 것입니다. 경계값 분석(Boundary Value Analysis)을 적용하여 `0`, `-1`, `MAX_VALUE` 등의 경계값 테스트를 추가하는 것을 권장합니다.

3. **검증 범위를 넓히세요.** "생성 후 조회" 패턴의 테스트에서 핵심 필드 하나만 검증하면, 나머지 필드의 바인딩/저장 오류를 놓칠 수 있습니다. 생성 시 전달한 모든 값이 조회 시에도 동일하게 반환되는지 확인하는 것이 인수 테스트의 역할입니다.
