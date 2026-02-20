package gift;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.empty;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductBehaviorTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * Behavior 4: 상품을 생성하면 조회 시 반환된다
     *
     * [현재 행동 — DTO 바인딩 버그]
     * CreateProductRequest에 setter가 없어 formParam 바인딩이 실패한다.
     * categoryId가 null로 남아 findById(null) → IllegalArgumentException → HTTP 500.
     * 상품이 생성되지 않으므로 조회 시 빈 목록이 반환된다.
     *
     * Given: 카테고리(id=1)가 존재
     * When:  POST /api/products (formParam: name, price, imageUrl, categoryId)
     * Then:  HTTP 500 (DTO 바인딩 실패) / GET /api/products → 빈 목록
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/product-setup.sql"})
    void should_fail_to_create_product_due_to_dto_binding_bug() {
        // When & Then — 생성 실패 (DTO에 setter 없음 → categoryId=null → findById(null) 예외)
        RestAssured.given()
            .formParam("name", "테스트상품")
            .formParam("price", 10000)
            .formParam("imageUrl", "http://image.url")
            .formParam("categoryId", 1)
        .when()
            .post("/api/products")
        .then()
            .statusCode(500);

        // Then — 후속 행동 검증: 상품이 생성되지 않았으므로 빈 목록
        RestAssured.given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("$", empty());
    }

    /**
     * Behavior 5: 존재하지 않는 카테고리로 상품 생성 시 실패한다
     *
     * Given: 카테고리가 존재하지 않음 (DB 비어 있음)
     * When:  POST /api/products (formParam: name, price, imageUrl, categoryId=9999)
     * Then:  HTTP 500 / GET /api/products → 빈 목록 (상품 미생성)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void should_fail_to_create_product_when_category_does_not_exist() {
        // When & Then — 존재하지 않는 카테고리로 생성 실패
        RestAssured.given()
            .formParam("name", "테스트상품")
            .formParam("price", 10000)
            .formParam("imageUrl", "http://image.url")
            .formParam("categoryId", 9999)
        .when()
            .post("/api/products")
        .then()
            .statusCode(500);

        // Then — 후속 행동 검증: 상품이 생성되지 않았으므로 빈 목록
        RestAssured.given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("$", empty());
    }
}
