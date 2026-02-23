package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("상품 생성 요청 시 200 응답과 자동 생성된 id가 반환된다")
    @Sql(scripts = "/data/product-acceptance/상품_생성_성공.sql", executionPhase = BEFORE_TEST_METHOD)
    void 상품_생성_성공() {
        // Given: 카테고리(id=1)가 존재하는 상태
        var request = Map.of(
                "name", "맥북 에어",
                "price", 1500000,
                "imageUrl", "https://example.com/macbook.png",
                "categoryId", 1
        );

        // When: 상품 생성 API 호출
        // Then: 200 OK, id가 자동 생성되어 반환된다
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/api/products")
                .then().log().all()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("name", equalTo("맥북 에어"))
                    .body("price", equalTo(1500000));
    }

    @Test
    @DisplayName("상품을 생성하면 목록 조회 시 포함된다")
    @Sql(scripts = "/data/product-acceptance/상품_생성_후_목록에서_조회된다.sql", executionPhase = BEFORE_TEST_METHOD)
    void 상품_생성_후_목록에서_조회된다() {
        // Given: 카테고리(id=1)가 존재하고 상품이 없는 상태
        var request = Map.of(
                "name", "맥북 에어",
                "price", 1500000,
                "imageUrl", "https://example.com/macbook.png",
                "categoryId", 1
        );

        // When: 상품 생성
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/api/products")
                .then().log().all()
                    .statusCode(200);

        // When: 상품 목록 조회
        // Then: 생성된 상품이 목록에 1건 존재한다 (상태 변화 증명)
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/products")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].name", equalTo("맥북 에어"))
                    .body("[0].price", equalTo(1500000));
    }

    @Test
    @DisplayName("상품이 여러 건 있을 때 전체 목록이 조회된다")
    @Sql(scripts = "/data/product-acceptance/상품_여러건_조회_성공.sql", executionPhase = BEFORE_TEST_METHOD)
    void 상품_여러건_조회_성공() {
        // Given: 3건의 상품이 존재하는 상태 (seed.sql 기반)

        // When: 상품 목록 조회 API 호출
        // Then: 200 OK, 3건 모두 반환되며 id와 name이 정확하다
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/products")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(3))
                    .body("id", hasItems(1, 2, 3))
                    .body("name", hasItems("맥북 에어", "아이패드", "나이키 후드티"));
    }

    @Test
    @DisplayName("상품이 없으면 빈 배열이 반환된다")
    @Sql(scripts = "/data/product-acceptance/상품이_없으면_빈_배열이_반환된다.sql", executionPhase = BEFORE_TEST_METHOD)
    void 상품이_없으면_빈_배열이_반환된다() {
        // Given: 상품이 없는 빈 상태

        // When: 상품 목록 조회 API 호출
        // Then: 200 OK, 빈 배열 반환
        RestAssured
                .given().log().all()
                .when()
                    .get("/api/products")
                .then().log().all()
                    .statusCode(200)
                    .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품 생성 시 실패한다")
    @Sql(scripts = "/data/product-acceptance/존재하지_않는_카테고리로_상품_생성_실패.sql", executionPhase = BEFORE_TEST_METHOD)
    void 존재하지_않는_카테고리로_상품_생성_실패() {
        // Given: 카테고리가 없는 빈 상태
        var request = Map.of(
                "name", "맥북 에어",
                "price", 1500000,
                "imageUrl", "https://example.com/macbook.png",
                "categoryId", 999
        );

        // When: 존재하지 않는 categoryId=999로 상품 생성 API 호출
        // Then: 서버 오류 (NoSuchElementException 발생)
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/api/products")
                .then().log().all()
                    .statusCode(500);
    }
}
