# Test Code Review Report

## 총평

테스트 격리 전략(`cleanup.sql` + `@SqlMergeMode(MERGE)`)과 한글 메서드 네이밍이 잘 갖춰져 있고, `GiftAcceptanceTest`의 재고 차감 시나리오가 체계적으로 구성되어 있습니다. 그러나 **중간 단계 응답 검증 누락**, **CLAUDE.md와의 불일치**, **테스트 커버리지 공백** 등 신뢰성과 유지보수 측면에서 개선이 필요한 부분이 발견됩니다.

---

## 발견된 문제점

### 🔴 치명적 #1 — 중간 단계 API 호출의 응답 상태 미검증

**파일:** `GiftAcceptanceTest.java:40-41`

```java
void 선물을_보낸_후_동일_옵션으로_다시_보내면_누적_차감된다() {
    // When: 3개 선물 후 4개 추가 선물 (총 7개 차감)
    선물_보내기(1L, 1L, 3, 2L);   // ← 응답 상태 미검증
    선물_보내기(1L, 1L, 4, 2L);   // ← 응답 상태 미검증

    // Then: 잔여 3개이므로 4개 요청은 실패해야 한다
    선물_보내기(1L, 1L, 4, 2L).then().statusCode(500);
    선물_보내기(1L, 1L, 3, 2L).then().statusCode(200);
}
```

첫 두 선물 요청이 **실패해도 테스트가 통과할 수 있습니다.** 만약 첫 번째 요청이 500으로 실패하면 재고가 10개 그대로이므로, 이후 4개 요청이 성공하여 **테스트의 전제 자체가 무너지지만 테스트는 여전히 통과합니다.** 테스트가 "누적 차감"을 검증한다고 선언하면서 정작 누적이 일어났는지 확인하지 않는 것은 false positive의 원인이 됩니다.

**같은 문제:** `CategoryAcceptanceTest.java:34-36`

```java
void 카테고리를_여러_개_생성하면_모두_목록에서_조회된다() {
    given().queryParam("name", "음료").when().post("/api/categories");   // ← 미검증
    given().queryParam("name", "디저트").when().post("/api/categories"); // ← 미검증
    given().queryParam("name", "케이크").when().post("/api/categories"); // ← 미검증

    // Then
    given().when().get("/api/categories").then()
            .statusCode(200)
            .body(".", hasSize(3));
}
```

**개선된 코드:**

```java
@Test
@Sql(scripts = "/sql/gift-data.sql", executionPhase = BEFORE_TEST_METHOD)
void 선물을_보낸_후_동일_옵션으로_다시_보내면_누적_차감된다() {
    // When: 3개 선물 후 4개 추가 선물 (총 7개 차감)
    선물_보내기(1L, 1L, 3, 2L).then().statusCode(200);  // 선행 조건 검증
    선물_보내기(1L, 1L, 4, 2L).then().statusCode(200);  // 선행 조건 검증

    // Then: 잔여 3개이므로 4개 요청은 실패해야 한다
    선물_보내기(1L, 1L, 4, 2L).then().statusCode(500);

    // 잔여 3개이므로 3개 요청은 성공해야 한다
    선물_보내기(1L, 1L, 3, 2L).then().statusCode(200);
}
```

---

### 🟡 경고 #1 — CLAUDE.md 테스트 규칙과 실제 구현의 불일치

**파일:** `AcceptanceTest.java`, `CLAUDE.md`

CLAUDE.md에 명시된 규칙:
> **인수 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + **`TestRestTemplate`**

실제 구현:
```java
// AcceptanceTest.java — RestAssured 사용
@BeforeEach
void setUp() {
    RestAssured.port = port;
}
```

`TestRestTemplate` 대신 `RestAssured`를 사용하고 있습니다. RestAssured가 기능적으로 더 표현력이 뛰어난 선택이지만, 문서화된 규칙과 실제 코드가 다르면 새로운 팀원이 혼란을 겪습니다.

**개선 방안:** 둘 중 하나를 선택하여 통일해야 합니다.

- RestAssured를 유지한다면 → CLAUDE.md의 테스트 규칙을 `RestAssured`로 갱신
- CLAUDE.md를 따른다면 → 테스트 코드를 `TestRestTemplate` 기반으로 변경

---

### 🟡 경고 #2 — SQL 스크립트 간 데이터 중복으로 인한 조합 불가

**파일:** `category-data.sql`, `product-data.sql`, `gift-data.sql`

```sql
-- category-data.sql
INSERT INTO category (id, name) VALUES (1, '음료');

-- product-data.sql (category 데이터 중복 포함)
INSERT INTO category (id, name) VALUES (1, '음료');          -- ← 중복!
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, ...);

-- gift-data.sql (category + product 데이터 중복 포함)
INSERT INTO category (id, name) VALUES (1, '음료');          -- ← 중복!
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, ...);  -- ← 중복!
INSERT INTO option (id, name, quantity, product_id) VALUES (1, 'Tall', 10, 1);
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
```

각 스크립트가 **자기 완결적(self-contained)**으로 설계되어 현재는 동작하지만, 두 스크립트를 조합해서 사용하는 것이 불가능합니다. 예를 들어, `@Sql({"/sql/category-data.sql", "/sql/product-data.sql"})`을 사용하면 `category.id=1`이 두 번 INSERT되어 **Primary Key 충돌**이 발생합니다.

테스트 시나리오가 복잡해질수록 스크립트 조합이 필요해지는데, 현재 구조에서는 매번 새로운 자기 완결 스크립트를 만들어야 합니다.

**개선 방안:** 계층적으로 분리하고 조합 가능하게 설계

```sql
-- category-data.sql (category만)
INSERT INTO category (id, name) VALUES (1, '음료');
INSERT INTO category (id, name) VALUES (2, '디저트');

-- product-data.sql (product만, category-data.sql에 의존)
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '아메리카노', 4500, '...', 1);

-- option-data.sql (option만, product-data.sql에 의존)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, 'Tall', 10, 1);

-- member-data.sql (member만)
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
```

사용 시:
```java
@Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql", "/sql/option-data.sql", "/sql/member-data.sql"})
```

---

### 🟡 경고 #3 — 실패 케이스에서 500 상태 코드 직접 검증

**파일:** `ProductAcceptanceTest.java:48`, `GiftAcceptanceTest.java:19,26,33,44`

```java
// ProductAcceptanceTest.java
.then().statusCode(500);

// GiftAcceptanceTest.java
선물_보내기(1L, 1L, 5, 2L).then().statusCode(500);
```

500 Internal Server Error를 **기대되는 동작**으로 검증하고 있습니다. 이는 현재 에러 핸들링이 미구현되어 어쩔 수 없지만, 다음 문제를 내포합니다:

- `NoSuchElementException`(존재하지 않는 엔티티)과 `IllegalStateException`(재고 부족)이 **동일한 500**으로 반환되어 실패 원인을 구분할 수 없음
- 에러 핸들링이 추가되면 (400, 404, 409 등) **모든 실패 테스트가 깨짐**
- DB 연결 실패 등 진짜 서버 에러와 비즈니스 예외를 구분할 수 없음

**개선 방안:** 에러 핸들링 구현 후 적절한 상태 코드로 변경

```java
// 존재하지 않는 리소스 → 404
선물_보내기(1L, 999L, 1, 2L).then().statusCode(404);

// 재고 부족 → 400 또는 409
선물_보내기(1L, 1L, 11, 2L).then().statusCode(400);

// 존재하지 않는 카테고리 → 404
given().queryParam("categoryId", 999)...then().statusCode(404);
```

---

### 🟡 경고 #4 — 하드코딩된 ID로 인한 SQL 스크립트 결합도

**파일:** 전체 테스트 클래스

```java
// ProductAcceptanceTest.java — categoryId=1이 category-data.sql의 ID와 결합
.queryParam("categoryId", 1)

// GiftAcceptanceTest.java — optionId=1, memberId=1L 등 모두 gift-data.sql과 결합
선물_보내기(1L, 1L, 7, 2L);
```

테스트 코드의 매직 넘버(`1L`, `2L`, `999L`)가 SQL 스크립트의 INSERT ID와 **암묵적으로 결합**되어 있습니다. SQL 스크립트의 ID를 변경하면 테스트가 깨지지만, 컴파일 에러가 발생하지 않아 원인 파악이 어렵습니다.

**개선 방안:** 테스트 클래스에 상수로 의도를 명시

```java
class GiftAcceptanceTest extends AcceptanceTest {
    // gift-data.sql에 정의된 테스트 픽스처 ID
    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long TALL_OPTION_ID = 1L;
    private static final int INITIAL_STOCK = 10;
    private static final Long NON_EXISTENT_OPTION_ID = 999L;

    @Test
    @Sql(scripts = "/sql/gift-data.sql", executionPhase = BEFORE_TEST_METHOD)
    void 재고보다_많은_수량으로_선물을_보내면_실패한다() {
        선물_보내기(SENDER_ID, TALL_OPTION_ID, INITIAL_STOCK + 1, RECEIVER_ID)
                .then().statusCode(500);
    }
}
```

---

### 🟡 경고 #5 — `카테고리를_여러_개_생성하면_모두_목록에서_조회된다` — 불완전한 검증

**파일:** `CategoryAcceptanceTest.java:32-45`

```java
void 카테고리를_여러_개_생성하면_모두_목록에서_조회된다() {
    given().queryParam("name", "음료").when().post("/api/categories");
    given().queryParam("name", "디저트").when().post("/api/categories");
    given().queryParam("name", "케이크").when().post("/api/categories");

    given().when().get("/api/categories").then()
            .statusCode(200)
            .body(".", hasSize(3));   // ← 개수만 검증, 내용 미검증
}
```

3개가 있다는 것만 확인하고, **실제로 "음료", "디저트", "케이크"가 맞는지 검증하지 않습니다.** 만약 서비스에 버그가 있어 모든 카테고리가 `null` 이름으로 저장되더라도 이 테스트는 통과합니다.

반면 `카테고리를_생성하면_목록에서_조회된다`는 `.body("[0].name", equalTo("음료"))`로 내용까지 검증하고 있어 두 테스트의 검증 수준이 불일치합니다.

**개선된 코드:**

```java
@Test
void 카테고리를_여러_개_생성하면_모두_목록에서_조회된다() {
    given().queryParam("name", "음료").when().post("/api/categories").then().statusCode(200);
    given().queryParam("name", "디저트").when().post("/api/categories").then().statusCode(200);
    given().queryParam("name", "케이크").when().post("/api/categories").then().statusCode(200);

    given()
    .when()
            .get("/api/categories")
    .then()
            .statusCode(200)
            .body(".", hasSize(3))
            .body("name", hasItems("음료", "디저트", "케이크"));
}
```

---

### 🟢 개선 권장 #1 — 누락된 테스트 커버리지

현재 테스트가 커버하지 않는 시나리오:

| 영역 | 누락된 테스트 케이스 |
|------|---------------------|
| Category | 빈 이름(`name=""`)으로 생성, `null` 이름으로 생성 |
| Product | 음수 가격(`price=-1`)으로 생성, `imageUrl` 없이 생성 |
| Gift | 수량 0으로 선물 보내기, 음수 수량으로 선물 보내기 |
| Gift | 자기 자신에게 선물 보내기 (`senderId == receiverId`) |
| Gift | 존재하지 않는 회원(`Member-Id: 999`)이 선물 보내기 |
| Wish | `WishService` 관련 테스트 전체 누락 |
| Option | `OptionService` 관련 테스트 전체 누락 |

특히 **음수 수량 선물**은 `Option.decrease()`의 보안 취약점과 직결되는 테스트입니다.

---

### 🟢 개선 권장 #2 — `AcceptanceTest` 베이스 클래스의 접근 제어자

**파일:** `AcceptanceTest.java:18-19`

```java
@LocalServerPort
private int port;
```

`port` 필드가 `private`이므로 하위 클래스에서 접근할 수 없습니다. 현재는 `@BeforeEach`에서 `RestAssured.port`에 설정하는 방식이라 문제가 없지만, 하위 클래스에서 포트 번호를 직접 사용해야 하는 경우(예: WebSocket 연결, 커스텀 URL 구성)에 확장이 불편합니다. 현 구조에서는 크게 문제되지 않으나, 향후 확장을 고려하면 `protected`가 적절합니다.

---

### 🟢 개선 권장 #3 — `cleanup.sql`에 IDENTITY 리셋 누락

**파일:** `cleanup.sql`

```sql
DELETE FROM wish;
DELETE FROM option;
DELETE FROM product;
DELETE FROM category;
DELETE FROM member;
```

DELETE만 수행하고 **AUTO_INCREMENT(IDENTITY) 시퀀스를 리셋하지 않습니다.** 테스트가 `id=1`을 하드코딩하고 있으므로, 만약 테스트 실행 순서가 바뀌어 이전 테스트에서 ID가 소비된 경우 SQL INSERT의 ID와 IDENTITY 시퀀스가 충돌할 수 있습니다.

현재는 SQL에서 `id`를 명시적으로 지정(`INSERT INTO category (id, name) VALUES (1, ...)`)하므로 H2에서 동작하지만, IDENTITY 전략과 명시적 ID 삽입의 혼용은 잠재적 불안정 요소입니다.

**개선 방안:**

```sql
DELETE FROM wish;
DELETE FROM option;
DELETE FROM product;
DELETE FROM category;
DELETE FROM member;

ALTER TABLE wish ALTER COLUMN id RESTART WITH 1;
ALTER TABLE option ALTER COLUMN id RESTART WITH 1;
ALTER TABLE product ALTER COLUMN id RESTART WITH 1;
ALTER TABLE category ALTER COLUMN id RESTART WITH 1;
ALTER TABLE member ALTER COLUMN id RESTART WITH 1;
```

---

## 추가 조언

1. **"Given-When-Then" 구간마다 선행 조건을 반드시 검증하세요.** 특히 When 단계에서 여러 API를 호출할 때, 각 호출의 성공 여부를 확인하지 않으면 테스트의 전제가 무너져도 알 수 없습니다. 이는 false positive (잘못된 성공)의 가장 흔한 원인입니다.

2. **SQL 스크립트는 조합 가능하게 설계하세요.** 도메인 엔티티별로 한 파일에 해당 엔티티의 데이터만 담고, 필요한 스크립트를 `@Sql`의 배열로 조합하는 방식이 유지보수에 유리합니다.

3. **에러 핸들링이 구현되면 테스트의 기대 상태 코드를 즉시 갱신하세요.** 현재 모든 실패를 `500`으로 검증하고 있어, `@RestControllerAdvice` 도입 시 실패 테스트가 대량 발생합니다. 이를 대비해 실패 테스트에 `// TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경` 주석을 달아두는 것도 방법입니다.

4. **인수 테스트에서 "상태 검증"을 더 강화하세요.** 현재 `GiftAcceptanceTest`는 재고 차감을 "이후 요청의 성공/실패"로 간접 검증합니다. GET `/api/options` 엔드포인트가 추가되면, 직접 잔여 수량을 조회하여 `quantity == 3` 등으로 검증하는 것이 더 명확합니다.
