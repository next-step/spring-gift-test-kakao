# 테스트 전략 문서

## 1. 검증할 행위 목록

### 선정 기준

행위 선정 시 다음 기준을 적용했다.

- **사용자 관점**: 내부 메서드 호출이 아닌, API 경계에서 관찰 가능한 결과를 검증한다.
- **비즈니스 가치**: 시스템의 핵심 기능(상품 관리, 선물 보내기, 재고 차감)을 우선한다.
- **리팩터링 안전망**: 내부 구조가 바뀌어도 깨지지 않고, 외부 행동이 바뀌면 반드시 깨지는 테스트를 작성한다.
- **실패 시나리오 포함**: 성공 경로뿐 아니라 비즈니스 규칙 위반 시의 행동도 보호 대상에 포함한다.

### 검증 대상 행위

| # | 행위 | 성공/실패 | 선정 이유 |
|---|------|-----------|-----------|
| 1 | 카테고리를 생성하면 목록에서 조회된다 | 성공 | 상품 등록의 전제 조건. 생성 → 조회 흐름이 정상 동작해야 한다. |
| 2 | 상품을 생성하면 목록에서 조회된다 | 성공 | 선물 보내기의 전제 조건. 카테고리 연관관계가 올바르게 설정되어야 한다. |
| 3 | 존재하지 않는 카테고리로 상품을 생성하면 실패한다 | 실패 | 잘못된 참조 데이터에 대한 시스템의 방어 행동을 보호한다. |
| 4 | 선물을 보내면 재고가 차감된다 | 성공 | **핵심 비즈니스 규칙**. `option.decrease()` 호출 여부가 아니라, 재고 감소 결과를 검증한다. |
| 5 | 재고보다 많은 수량으로 선물을 보내면 실패한다 | 실패 | 재고 부족 시 시스템이 요청을 거부하는 행동을 보호한다. |
| 6 | 존재하지 않는 옵션으로 선물을 보내면 실패한다 | 실패 | 잘못된 옵션 ID에 대한 방어 행동을 보호한다. |
| 7 | 선물을 보낸 후 동일 옵션으로 다시 보내면 누적 차감된다 | 성공 | 재고 차감이 트랜잭션 단위로 올바르게 영속되는지 검증한다. |
| 8 | 카테고리를 여러 개 생성하면 모두 목록에서 조회된다 | 성공 | 목록 조회가 전체 데이터를 반환하는지 확인한다. |

---

## 2. 테스트 데이터 전략

### 테스트 환경

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + **RestAssured**로 실제 서블릿 컨테이너를 띄우고 HTTP 요청을 보낸다.
- H2 인메모리 DB를 사용하며, 테스트 시작 시 스키마가 자동 생성된다.

### 데이터 준비 방식 (Given 단계)

**`@Sql` 어노테이션을 사용**하여 SQL 스크립트로 테스트 데이터를 준비한다.

#### `@Sql` 선택 이유

| 비교 항목 | API 호출 / Service 호출 | `@Sql` |
|-----------|------------------------|--------|
| 테스트 의도 | Given 단계에 코드가 섞여 가독성 저하 | **SQL 파일로 분리되어 Given이 명확** |
| 실행 속도 | HTTP 왕복 또는 Service 레이어 경유 | **DB 직접 INSERT로 빠름** |
| API 미존재 엔티티 | Service/Repository 의존 필요 | **SQL로 동일하게 준비 가능** |
| 데이터 일관성 | 코드 변경 시 깨질 수 있음 | **스키마 기반으로 안정적** |

#### SQL 스크립트 구성

테스트별로 필요한 데이터를 SQL 파일로 관리한다.

```
src/test/resources/sql/
├── cleanup.sql                  # 전체 데이터 정리 (모든 테스트 전 실행)
├── category-data.sql            # 카테고리 테스트 데이터
├── product-data.sql             # 상품 테스트 데이터 (카테고리 포함)
└── gift-data.sql                # 선물 테스트 데이터 (카테고리 + 상품 + 옵션 + 회원)
```

#### SQL 스크립트 예시

**cleanup.sql** — 외래키 의존 역순으로 삭제
```sql
DELETE FROM wish;
DELETE FROM option;
DELETE FROM product;
DELETE FROM category;
DELETE FROM member;
```

**gift-data.sql** — 선물 보내기 시나리오에 필요한 전체 데이터
```sql
INSERT INTO category (id, name) VALUES (1, '음료');
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '아메리카노', 4500, 'https://example.com/img.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, 'Tall', 10, 1);
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
```

#### 테스트 메서드에서의 사용

```java
@Test
@Sql(scripts = "/sql/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/gift-data.sql", executionPhase = BEFORE_TEST_METHOD)
void 선물을_보내면_재고가_차감된다() {
    // Given: SQL로 이미 준비됨
    // When: API 호출
    // Then: 결과 검증
}
```

#### 엔티티별 데이터 준비 방법 (통일)

`@Sql`을 사용하면 API 존재 여부와 무관하게 모든 엔티티를 동일한 방식으로 준비할 수 있다.

| 엔티티 | API | Service | 준비 방법 |
|--------|-----|---------|-----------|
| Category | O | O | `@Sql` — INSERT INTO category |
| Product | O | O | `@Sql` — INSERT INTO product |
| Option | X | O | `@Sql` — INSERT INTO option |
| Member | X | X | `@Sql` — INSERT INTO member |
| Wish | X | O | 검증 대상이 아니므로 제외 |

### 테스트 간 격리 전략

- **`@DirtiesContext`는 사용하지 않는다.** 컨텍스트 재생성 비용이 크다.
- **`@Transactional`은 사용하지 않는다.** RestAssured는 별도 스레드에서 HTTP 요청을 보내므로, 테스트의 `@Transactional` 롤백이 적용되지 않는다.
- **`@Sql`의 `executionPhase = BEFORE_TEST_METHOD`로 격리한다.** 매 테스트 실행 전 `cleanup.sql`을 먼저 실행하여 이전 테스트의 데이터를 정리한 뒤, 필요한 데이터를 INSERT한다.

```java
@Sql(scripts = "/sql/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/gift-data.sql", executionPhase = BEFORE_TEST_METHOD)
```

이를 통해 `@BeforeEach`에서 Repository를 주입받아 정리할 필요가 없어지고, 테스트 클래스가 데이터 준비 코드로부터 깔끔하게 분리된다.

---

## 3. 검증 전략

### 기본 원칙

> "무엇을 호출했는가"가 아니라 **"결과가 어떠한가"**를 검증한다.

### 검증 방식

#### HTTP 응답으로 검증하는 항목

| 검증 대상 | 검증 방법 |
|-----------|-----------|
| 생성 성공 | 응답 코드 200, 응답 본문에 생성된 엔티티 정보 포함 |
| 생성 실패 (잘못된 참조) | 응답 코드 5xx (현재 에러 핸들링 미구현) |
| 선물 보내기 성공 | 응답 코드 200 |
| 선물 보내기 실패 | 응답 코드 5xx |

#### "다음 행동"으로 이전 행동을 검증하는 전략

DB 직접 조회 없이 API만으로 상태 변화를 확인하는 것이 원칙이다.

| 검증하고 싶은 것 | 검증 방법 |
|------------------|-----------|
| 카테고리가 생성되었는가 | `POST` 후 `GET /api/categories`로 목록 조회하여 확인 |
| 상품이 생성되었는가 | `POST` 후 `GET /api/products`로 목록 조회하여 확인 |
| 재고가 차감되었는가 | **API로 검증 불가** — Option 조회 API가 없음 |

#### 재고 차감 검증의 특수성

Option 조회 REST API가 존재하지 않으므로, 재고 차감은 **간접 검증(행동 기반)**으로 검증한다.

- **방법**: `@Sql`로 재고 10개인 옵션을 준비한 뒤, 7개를 선물하고, 다시 5개를 선물하면 실패해야 한다. (잔여 재고 3개 < 요청 5개)
- **장점**: API 경계만으로 검증하므로 내부 구현 변경에 강하다. `decrease()` 호출 여부가 아닌, 재고 부족 시 선물이 거부되는 **행동**을 보호한다.

---

## 4. 주요 의사결정

### `@SpringBootTest` + RestAssured 선택 이유

| 비교 항목 | MockMvc | RestAssured |
|-----------|---------|-------------|
| 서블릿 컨테이너 | 미구동 (Mock) | **실제 구동** |
| HTTP 요청 | 모킹된 요청 | **실제 HTTP 요청** |
| 직렬화/역직렬화 | 부분 검증 | **전체 경로 검증** |
| 트랜잭션 경계 | 테스트와 공유 가능 | **분리됨 (실제와 동일)** |
| API 테스트 가독성 | 직접 구성 필요 | **Given-When-Then DSL 내장** |
| 응답 검증 | 별도 역직렬화 필요 | **JsonPath + Hamcrest 체이닝** |

**RestAssured를 선택한다.** 인수 테스트의 목적은 실제 HTTP 요청부터 DB 영속까지의 전체 흐름을 검증하는 것이다. 특히 `spring.jpa.open-in-view=false` 설정으로 인한 트랜잭션 경계 이슈를 MockMvc로는 발견할 수 없다. RestAssured의 `given()-when()-then()` DSL은 인수 테스트의 시나리오 구조와 자연스럽게 대응되며, 응답 본문을 JsonPath로 바로 검증할 수 있어 별도의 역직렬화 코드가 필요 없다.

### 컨트롤러 미존재 기능의 테스트 접근

`@Sql`을 사용함으로써 API/Service 존재 여부와 무관하게 모든 엔티티를 SQL INSERT로 동일하게 준비한다.

- **Option, Member**: SQL 스크립트에서 직접 INSERT. Service나 Repository를 테스트 코드에 주입할 필요가 없다.
- **Wish**: 현재 선물 보내기 흐름과 무관하므로 이번 테스트 범위에서 **제외**한다.

### 테스트 독립성

- 각 테스트는 **순서에 의존하지 않는다.** `@Sql`로 매 테스트 전 데이터를 초기화하고 필요한 데이터를 새로 준비한다.
- 하나의 테스트 메서드 안에서 시나리오를 구성할 때는 API 호출 순서가 의미를 갖는다. (예: 선물 보내기 → 재고 초과 선물 시도 → 실패 확인)

### Form Param vs JSON Body 불일치 처리

현재 API의 바인딩 방식이 일관되지 않다.

| 엔드포인트 | 바인딩 방식 |
|-----------|------------|
| `POST /api/categories` | Form/Query Parameter (`@ModelAttribute` 암시) |
| `POST /api/products` | Form/Query Parameter (`@ModelAttribute` 암시) |
| `POST /api/gifts` | JSON Body (`@RequestBody`) |

테스트에서는 각 API의 **현재 동작 방식을 그대로 반영**한다. 이 불일치 자체가 리팩터링 대상이 될 수 있으며, 인수 테스트는 리팩터링 전후로 외부 행동이 동일한지를 보장하는 역할을 한다.

```java
// 카테고리 생성 — Query Parameter
given()
        .queryParam("name", "음료")
.when()
        .post("/api/categories")
.then()
        .statusCode(200);

// 선물 보내기 — JSON Body
given()
        .header("Member-Id", memberId)
        .contentType("application/json")
        .body(giftRequestJson)
.when()
        .post("/api/gifts")
.then()
        .statusCode(200);
```

---

## 5. 테스트 구조

### 테스트 클래스 구성

```
src/test/java/gift/
├── AcceptanceTest.java          # 공통 설정 (베이스 클래스)
├── CategoryAcceptanceTest.java  # 카테고리 행위 검증
├── ProductAcceptanceTest.java   # 상품 행위 검증
└── GiftAcceptanceTest.java      # 선물 보내기 행위 검증
```

### 베이스 클래스

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
@SqlMergeMode(MERGE)
public abstract class AcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

- `@LocalServerPort`로 랜덤 포트를 주입받아 `RestAssured.port`에 설정한다.
- `@SqlMergeMode(MERGE)`를 선언하여, 자식 클래스의 method-level `@Sql`이 class-level `@Sql`을 override하지 않고 **병합**되도록 한다. 이를 통해 `cleanup.sql`은 항상 실행되고, 각 테스트는 데이터 SQL만 추가하면 된다.

```java
// 자식 클래스 예시 — cleanup.sql이 먼저 실행된 후 gift-data.sql이 실행된다
@Sql(scripts = "/sql/gift-data.sql", executionPhase = BEFORE_TEST_METHOD)
@Test
void 선물을_보내면_재고가_차감된다() { ... }
```

### 테스트 메서드 네이밍

한글 메서드명을 사용하여 행위를 명확히 표현한다.

```java
@Test
void 카테고리를_생성하면_목록에서_조회된다() { ... }

@Test
void 선물을_보내면_재고가_차감된다() { ... }

@Test
void 재고보다_많은_수량으로_선물을_보내면_실패한다() { ... }
```
