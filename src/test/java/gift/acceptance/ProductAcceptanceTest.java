package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * 검증 행위:
 * 3. 유효한 카테고리로 상품을 생성하면 목록에서 확인할 수 있다
 * 4. 여러 상품을 생성하면 모두 목록에 나타난다
 */
class ProductAcceptanceTest extends AcceptanceTest {

    private Long 카테고리_생성(final String name) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "%s"}
                        """, name))
                .post("/api/categories")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @Test
    void 유효한_카테고리로_상품을_생성하면_목록에서_확인할_수_있다() {
        Long categoryId = 카테고리_생성("전자기기");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "노트북", "price": 1500000, "imageUrl": "https://example.com/laptop.jpg", "categoryId": %d}
                        """, categoryId))
                .when()
                .post("/api/products")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("name", hasItem("노트북"));
    }

    @Test
    void 여러_상품을_생성하면_모두_목록에_나타난다() {
        Long categoryId = 카테고리_생성("전자기기");

        RestAssured.given().contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "노트북", "price": 1500000, "imageUrl": "https://example.com/laptop.jpg", "categoryId": %d}
                        """, categoryId))
                .post("/api/products");

        RestAssured.given().contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "스마트폰", "price": 900000, "imageUrl": "https://example.com/phone.jpg", "categoryId": %d}
                        """, categoryId))
                .post("/api/products");

        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("name", hasItems("노트북", "스마트폰"))
                .body("", hasSize(2));
    }
}