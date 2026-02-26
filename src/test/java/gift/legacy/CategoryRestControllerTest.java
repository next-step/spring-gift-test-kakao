package gift.legacy;

import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.response.ValidatableResponse;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CategoryRestControllerTest {

    private static final String CATEGORY_NAME = "식품";

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("유효한 이름으로 카테고리를 생성하면 200 OK와 생성된 카테고리를 반환한다")
    void createValidNameReturnsCreatedCategory() {
        createCategory(CATEGORY_NAME)
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo(CATEGORY_NAME));
    }

    @Test
    @DisplayName("카테고리가 존재할 때 목록을 조회하면 전체 카테고리를 반환한다")
    void retrieveCategoriesReturnsList() {
        // given
        createCategory("교환권").statusCode(200);
        createCategory("상품권").statusCode(200);

        // when & then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("name", hasItem("교환권"))
            .body("name", hasItem("상품권"));
    }

    @Test
    @DisplayName("동일한 이름으로 카테고리를 여러 개 생성하면 모두 저장된다")
    void createDuplicateNameAllowsMultiple() {
        // given
        createCategory(CATEGORY_NAME).statusCode(200);

        // when
        createCategory(CATEGORY_NAME).statusCode(200);

        // then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }

    private ValidatableResponse createCategory(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then();
    }
}
