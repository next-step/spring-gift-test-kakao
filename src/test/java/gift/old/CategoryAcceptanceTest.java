package gift.old;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

class CategoryAcceptanceTest extends AcceptanceTest {

    @Test
    void 카테고리를_생성하면_목록에서_조회된다() {
        // When
        given()
                .queryParam("name", "음료")
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200);

        // Then
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body(".", hasSize(1))
                .body("[0].name", equalTo("음료"));
    }

    @Test
    void 카테고리를_여러_개_생성하면_모두_목록에서_조회된다() {
        // When
        given().queryParam("name", "음료").when().post("/api/categories").then().statusCode(200);
        given().queryParam("name", "디저트").when().post("/api/categories").then().statusCode(200);
        given().queryParam("name", "케이크").when().post("/api/categories").then().statusCode(200);

        // Then
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body(".", hasSize(3))
                .body("name", hasItems("음료", "디저트", "케이크"));
    }
}
