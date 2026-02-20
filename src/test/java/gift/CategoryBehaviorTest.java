package gift;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

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
     * [현재 행동 — DTO 바인딩 버그]
     * CreateCategoryRequest에 setter가 없어 formParam 바인딩이 실패한다.
     * name이 null로 남아 Category(null)이 저장된다.
     * 생성은 성공(HTTP 200)하지만, name이 null인 카테고리가 만들어진다.
     *
     * Given: 없음 (사전 조건 없음)
     * When:  POST /api/categories (formParam: name=테스트카테고리)
     * Then:  HTTP 200 + 카테고리 생성됨 (name=null) / GET /api/categories → 목록에 포함 (name=null)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void should_create_category_and_return_in_list() {
        // When & Then — 생성 성공 (HTTP 200), 단 name은 null (DTO에 setter 없음)
        RestAssured.given()
                .formParam("name", "테스트카테고리")
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
//                .body("name", equalTo(null));

        // Then — 후속 행동 검증: GET /api/categories 에서 목록에 포함 (name=null)
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));
//            .body("[0].name", equalTo(null));
    }
}
