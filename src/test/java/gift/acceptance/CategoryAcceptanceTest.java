package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * 검증 행위:
 * 1. 카테고리를 생성하면 목록에서 확인할 수 있다
 * 2. 여러 카테고리를 생성하면 모두 목록에 나타난다
 */
class CategoryAcceptanceTest extends AcceptanceTest {

    @Test
    void 카테고리를_생성하면_목록에서_확인할_수_있다() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "전자기기"}
                        """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("name", hasItem("전자기기"));
    }

    @Test
    void 여러_카테고리를_생성하면_모두_목록에_나타난다() {
        RestAssured.given().contentType(ContentType.JSON)
                .body("""
                        {"name": "전자기기"}
                        """)
                .post("/api/categories");

        RestAssured.given().contentType(ContentType.JSON)
                .body("""
                        {"name": "패션"}
                        """)
                .post("/api/categories");

        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("name", hasItems("전자기기", "패션"))
                .body("", hasSize(2));
    }
}