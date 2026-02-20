# Spring Gift 인수테스트 가이드

## 프로젝트 정보

- Java 21, Spring Boot 3.5.8, JPA, H2
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`

## 인수테스트 작성 규칙

### 테스트 위치 및 네이밍

```
src/test/java/gift/
└── acceptance/
    ├── CategoryAcceptanceTest.java
    ├── ProductAcceptanceTest.java
    ├── GiftAcceptanceTest.java
    └── WishAcceptanceTest.java
```

- 클래스명: `*AcceptanceTest`
- 메서드명: 한글로 시나리오 설명 (예: `상품_등록_성공()`)

### 테스트 프레임워크

RestAssured를 사용한다. 의존성 추가 필요:

```groovy
testImplementation 'io.rest-assured:rest-assured:5.4.0'
```

### 기본 테스트 구조

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 카테고리_생성_성공() {
        // given
        var request = Map.of("name", "음료");

        // when
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/categories")
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }
}
```

### 헬퍼 메서드 패턴

반복되는 API 호출은 헬퍼 메서드로 추출한다:

```java
// 카테고리 생성 후 ID 반환
Long 카테고리_생성(String name) {
    return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
            .when()
            .post("/api/categories")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath().getLong("id");
}

// 상품 생성 후 ID 반환
Long 상품_생성(String name, int price, Long categoryId) {
    return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", "http://example.com/image.jpg",
                "categoryId", categoryId
            ))
            .when()
            .post("/api/products")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath().getLong("id");
}

// 선물 전달
ExtractableResponse<Response> 선물_전달(Long optionId, int quantity, Long receiverId, String message, Long senderId) {
    return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", senderId)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", receiverId,
                "message", message
            ))
            .when()
            .post("/api/gifts")
            .then()
            .extract();
}
```

### 데이터 격리

각 테스트는 독립적으로 실행되어야 한다:

```java
@BeforeEach
void setUp() {
    RestAssured.port = port;
    // 필요시 데이터 초기화
}
```

또는 `@Sql` 어노테이션으로 테스트 데이터 설정:

```java
@Sql("/test-data.sql")
@Test
void 테스트() { ... }
```

## 주요 인수테스트 시나리오

### 카테고리 API

| 시나리오       | 검증            |
|------------|---------------|
| 카테고리 목록 조회 | 200 OK, 목록 반환 |

### 상품 API

| 시나리오             | 검증                        |
|------------------|---------------------------|
| 존재하지 않는 카테고리로 생성 | 500 Internal Server Error |
| 상품 목록 조회         | 200 OK, 목록 반환             |

### 선물 API (핵심)

| 시나리오       | 검증                        |
|------------|---------------------------|
| 선물 전달 성공   | 200 OK, 재고 차감             |
| 재고 부족      | 500 Internal Server Error |
| 존재하지 않는 옵션 | 500 Internal Server Error |

### E2E 시나리오

```java
@Test
void 상품_등록부터_선물하기_전체_흐름() {
    // given
    var categoryId = 카테고리_생성("음료");
    var productId = 상품_생성("아메리카노", 4500, categoryId);
    var optionId = 옵션_생성(productId, "ICE", 10);

    // when
    var response = 선물_전달(optionId, 2, receiverId, "맛있게 드세요", senderId);

    // then
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(옵션_재고_조회(optionId)).isEqualTo(8);
}

@Test
void 재고_부족시_선물_실패() {
    // given
    var optionId = 옵션_생성(productId, "HOT", 1);

    // when
    var response = 선물_전달(optionId, 2, receiverId, "선물", senderId);

    // then
    assertThat(response.statusCode()).isEqualTo(500);
}
```

## API 엔드포인트 참고

| 메서드  | 경로              | 설명      | 헤더        |
|------|-----------------|---------|-----------|
| POST | /api/categories | 카테고리 생성 | -         |
| GET  | /api/categories | 카테고리 목록 | -         |
| POST | /api/products   | 상품 생성   | -         |
| GET  | /api/products   | 상품 목록   | -         |
| POST | /api/gifts      | 선물 전달   | Member-Id |

## 도메인 관계

```
Category 1 --- N Product 1 --- N Option
                Product 1 --- N Wish
Member   1 --- N Wish
```