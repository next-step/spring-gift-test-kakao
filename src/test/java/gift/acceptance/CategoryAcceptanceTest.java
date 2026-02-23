package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("classpath:sql/truncate.sql")
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 카테고리를_생성하면_목록_조회_시_해당_카테고리가_포함된다() {
        String name = "새카테고리";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem(name));
    }

    @Test
    @Sql("classpath:sql/test-data.sql")
    void 카테고리_목록을_조회한다() {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem("테스트카테고리"));
    }
}
