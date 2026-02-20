package gift.acceptance.category;

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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
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
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "간식"))
        .when()
                .post("/api/categories");

        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("name", hasItem("간식"));
    }

    @Test
    void 카테고리가_없으면_빈_목록을_반환한다() {
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("$", empty());
    }
}
