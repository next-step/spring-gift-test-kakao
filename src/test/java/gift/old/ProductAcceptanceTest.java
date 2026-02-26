package gift.old;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

class ProductAcceptanceTest extends AcceptanceTest {

    private static final long CATEGORY_ID = 1L;
    private static final long NON_EXISTENT_CATEGORY_ID = 999L;

    @Test
    @Sql(scripts = "/sql/category-data.sql", executionPhase = BEFORE_TEST_METHOD)
    void 상품을_생성하면_목록에서_조회된다() {
        // When
        given()
                .queryParam("name", "아메리카노")
                .queryParam("price", 4500)
                .queryParam("imageUrl", "https://example.com/img.jpg")
                .queryParam("categoryId", CATEGORY_ID)
        .when()
                .post("/api/products")
        .then()
                .statusCode(200);

        // Then
        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body(".", hasSize(1))
                .body("[0].name", equalTo("아메리카노"));
    }

    @Test
    void 존재하지_않는_카테고리로_상품을_생성하면_실패한다() {
        // When & Then
        given()
                .queryParam("name", "아메리카노")
                .queryParam("price", 4500)
                .queryParam("imageUrl", "https://example.com/img.jpg")
                .queryParam("categoryId", NON_EXISTENT_CATEGORY_ID)
        .when()
                .post("/api/products")
        .then()
                .statusCode(500); // TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경
    }
}
