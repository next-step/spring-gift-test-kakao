package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class CategoryBehaviorTest extends BaseBehaviorTest {

    /**
     * Behavior 6: 카테고리를 생성하면 조회 시 반환된다
     *
     * Given: 없음 (사전 조건 없음)
     * When:  POST /api/categories (JSON body: name=테스트카테고리)
     * Then:  HTTP 200 + 응답에 id, name 포함
     *        GET /api/categories → 목록에 동일한 카테고리 포함 (id, name 일치)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void 사전조건이_없을_때_카테고리를_생성하면_조회_목록에_포함된다() {
        // When & Then — 생성 성공, 응답 바디의 모든 필드 검증
        int createdId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "테스트카테고리"))
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("테스트카테고리"))
                .extract()
                .path("id");

        // Then — 후속 행동 검증: GET /api/categories 에서 생성한 카테고리가 동일한 id, name으로 조회
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(createdId))
                .body("[0].name", equalTo("테스트카테고리"));
    }
}
