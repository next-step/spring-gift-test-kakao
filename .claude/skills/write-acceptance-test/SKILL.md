---
name: write-acceptance-test
description: 인수 테스트 메서드 하나를 작성할 때 사용한다
argument-hint: 검증할 행위 (예: "위시리스트 추가 시 조회에 포함된다")
---

$ARGUMENTS에 대한 인수 테스트를 작성한다. 아래 단계를 순서대로 수행한다.

## 1단계: 테스트 전략 수립

- 검증할 외부 행위를 한 문장으로 정의한다
- 대상 엔드포인트(HTTP method + path)를 확인한다
- 성공/실패 시나리오를 구분한다
- 검증 방법을 결정한다 (HTTP 응답, DB 상태, 또는 둘 다)

## 2단계: 테스트 데이터 준비

- 테스트에 필요한 엔티티와 FK 관계를 파악한다
- 기존 SQL 데이터 파일에 추가할지, 새 파일이 필요한지 판단한다
- src/test/resources/sql/ 에 INSERT문을 작성하고 고정 ID를 부여한다
- cleanup.sql에 해당 테이블이 포함되어 있는지 확인한다

## 3단계: 테스트 메서드 작성

- AcceptanceTestBase를 상속한 클래스에 작성한다
- @Sql로 cleanup + data 스크립트를 연결한다
- @DisplayName에 검증할 행위를 한글로 서술한다
- RestAssured로 요청을 구성한다 (method, header, content type, body)
- 응답을 검증한다 (상태 코드, 바디)
- 필요시 Repository로 DB 상태를 추가 검증한다

## 4단계: 검증

- ./gradlew test를 실행하여 새 테스트가 통과하는지 확인한다
- 기존 테스트가 깨지지 않았는지 확인한다

## 예시: "선물을 보내면 옵션 재고가 감소한다"

### 1단계 결과

- 행위: 선물을 보내면 옵션 재고가 감소한다
- 엔드포인트: POST /api/gifts
- 시나리오: 성공
- 검증 방법: HTTP 200 응답 + DB에서 재고 감소 확인

### 2단계 결과 — gift-data.sql

선물 전송에는 카테고리 → 상품 → 옵션, 그리고 보내는 사람/받는 사람이 필요하다.

```sql
INSERT INTO category (id, name) VALUES (1, '식품');
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '떡볶이', 5000, 'http://example.com/image.png', 1);
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 10, 1);
```

### 3단계 결과 — GiftAcceptanceTest.java

```java
@Sql(scripts = {"/sql/cleanup.sql", "/sql/gift-data.sql"})
class GiftAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private OptionRepository optionRepository;

    @DisplayName("선물을 보내면 옵션 재고가 감소한다")
    @Test
    void giftDecreasesOptionQuantity() {
        // when: 옵션 1(재고 10)으로 수량 3 선물 전송
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body(Map.of(
                        "optionId", 1L,
                        "quantity", 3,
                        "receiverId", 2L,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);

        // then: 재고가 10에서 7로 감소
        Option updated = optionRepository.findById(1L).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }
}
```
