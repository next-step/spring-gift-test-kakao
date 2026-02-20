package gift.acceptance.product;

import gift.support.DatabaseCleanup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }

    Long 카테고리를_생성하고_ID를_반환한다(String name) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @Test
    void 상품을_생성한다() {
        Long categoryId = 카테고리를_생성하고_ID를_반환한다("음료");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("아메리카노"))
                .body("price", equalTo(4500))
                .body("imageUrl", equalTo("https://example.com/image.png"));
    }

    @Test
    void 상품을_생성하면_조회_목록에_포함된다() {
        Long categoryId = 카테고리를_생성하고_ID를_반환한다("음료");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products");

        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("name", hasItem("아메리카노"));
    }

    @Test
    void 존재하지_않는_카테고리로_상품을_생성하면_실패한다() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", 999999
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(500);
    }
}