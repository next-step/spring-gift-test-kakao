package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class ProductBehaviorTest extends BaseBehaviorTest {

    /**
     * Behavior 4: 상품을 생성하면 조회 시 반환된다
     *
     * Given: 카테고리(id=1, name=테스트카테고리)가 존재
     * When:  POST /api/products (JSON body: name, price, imageUrl, categoryId)
     * Then:  HTTP 200 + 응답에 모든 필드(name, price, imageUrl, category) 포함
     *        GET /api/products → 목록에 동일한 상품 포함
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/product-setup.sql"})
    void 카테고리가_존재할_때_상품을_생성하면_조회_목록에_포함된다() {
        // When & Then — 생성 성공, 응답 바디의 모든 필드 검증
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "테스트상품",
                        "price", 10000,
                        "imageUrl", "http://image.url",
                        "categoryId", 1
                ))
                .when()
                .post("/api/products")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("테스트상품"))
                .body("price", equalTo(10000))
                .body("imageUrl", equalTo("http://image.url"))
                .body("category.id", equalTo(1))
                .body("category.name", equalTo("테스트카테고리"));

        // Then — 후속 행동 검증: GET /api/products 에서 동일한 상품이 모든 필드와 함께 조회
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("테스트상품"))
                .body("[0].price", equalTo(10000))
                .body("[0].imageUrl", equalTo("http://image.url"))
                .body("[0].category.id", equalTo(1))
                .body("[0].category.name", equalTo("테스트카테고리"));
    }

    /**
     * Behavior 5: 존재하지 않는 카테고리로 상품 생성 시 실패한다
     *
     * Given: 카테고리가 존재하지 않음 (DB 비어 있음)
     * When:  POST /api/products (JSON body: categoryId=9999)
     * Then:  HTTP 500 / GET /api/products → 빈 목록 (상품 미생성)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void 카테고리가_존재하지_않을_때_상품을_생성하면_실패한다() {
        // When & Then — 존재하지 않는 카테고리로 생성 실패
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "테스트상품",
                        "price", 10000,
                        "imageUrl", "http://image.url",
                        "categoryId", 9999
                ))
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

