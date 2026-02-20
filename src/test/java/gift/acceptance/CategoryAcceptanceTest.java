package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 카테고리를_생성한다() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "음료"))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("음료"));
    }

    @Test
    void 카테고리를_생성하면_조회_목록에_포함된다() {
        // given
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "간식"))
        .when()
                .post("/api/categories");

        // when & then
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("name", hasItem("간식"));
    }
}