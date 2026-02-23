package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryBehaviorTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * Behavior 6: 카테고리를 생성하면 조회 시 반환된다
     *
     * Controller에 @RequestBody가 붙어 있어 JSON으로 요청해야 한다.
     * CreateCategoryRequest에 setter가 존재하므로 Jackson 역직렬화가 정상 동작한다.
     *
     * Given: 없음 (사전 조건 없음)
     * When:  POST /api/categories (JSON Body: {"name": "테스트카테고리"})
     * Then:  HTTP 200 + 카테고리 생성됨 (name=테스트카테고리) / GET /api/categories → 목록에 포함
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void 카테고리_생성하면_조회_시_반환된다() {
        // When & Then — 생성 성공 (HTTP 200)
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"테스트카테고리\"}")
                .when().post("/api/categories")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("테스트카테고리"));

        // Then — 후속 행동 검증: GET /api/categories 에서 목록에 포함
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("테스트카테고리"));
    }
}
