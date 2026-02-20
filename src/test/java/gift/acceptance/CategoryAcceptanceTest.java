package gift.acceptance;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

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
    void 카테고리를_정상적으로_생성한다() {
        String name = "새카테고리";

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("name", name)
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

    @Test
    void 이름이_null이면_카테고리_생성에_실패한다() {
        given()
            .contentType("application/x-www-form-urlencoded")
        .when()
            .post("/api/categories")
        .then()
            .statusCode(not(200));
    }

    @Test
    void 이름이_빈_문자열이면_카테고리_생성에_실패한다() {
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("name", "")
        .when()
            .post("/api/categories")
        .then()
            .statusCode(not(200));
    }

    @Test
    @Sql("classpath:sql/test-data.sql")
    void 중복된_이름의_카테고리_생성에_실패한다() {
        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("name", "테스트카테고리")
        .when()
            .post("/api/categories")
        .then()
            .statusCode(not(200));
    }
}
