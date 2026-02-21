package gift.acceptance;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@Sql(scripts = {"/sql/cleanup.sql", "/sql/product-data.sql"})
class ProductAcceptanceTest extends AcceptanceTestBase {

    @DisplayName("상품을 생성하면 조회 시 포함된다")
    @Test
    void createAndRetrieveProduct() {
        // when & then: 조회 시 생성한 상품이 포함된다
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("name", hasItem("떡볶이"))
                .body("", hasSize(1))
                .body("[0].price", equalTo(5000))
                .body("[0].category.name", equalTo("식품"));
    }

    @DisplayName("존재하지 않는 카테고리로 상품 생성 시 실패한다")
    @Test
    void createProductWithInvalidCategory() {
        // when & then: 존재하지 않는 카테고리로 상품 생성 요청 시 서버 에러 발생
        RestAssured.given()
                .param("name", "떡볶이")
                .param("price", 5000)
                .param("imageUrl", "http://example.com/image.png")
                .param("categoryId", 9999)
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);
    }
}
