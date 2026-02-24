package gift;

import gift.model.Category;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class CategoryApiTest extends ApiTest {

    @Test
    @DisplayName("카테고리를 생성하면 응답에 id가 포함된다")
    void create_jsonBody_responds200WithId() {
        // Act & Assert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "교환권"}
                        """)
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("카테고리 목록을 조회하면 등록된 카테고리가 모두 나타난다")
    void retrieve_afterSeed_returnsCategoryList() {
        // Arrange
        categoryRepository.save(new Category("교환권"));
        categoryRepository.save(new Category("상품권"));

        // Act & Assert
        RestAssured.given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("교환권", "상품권"));
    }
}
