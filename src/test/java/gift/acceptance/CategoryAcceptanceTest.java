package gift.acceptance;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@Sql(scripts = {"/sql/cleanup.sql", "/sql/category-data.sql"})
class CategoryAcceptanceTest extends AcceptanceTestBase {

    @DisplayName("카테고리를 생성하면 조회 시 포함된다")
    @Test
    void createAndRetrieveCategory() {
        // when & then: 조회 시 생성한 카테고리가 포함된다
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("name", hasItem("식품"))
                .body("", hasSize(1));
    }
}
