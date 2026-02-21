package gift;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import gift.application.request.CreateCategoryRequest;
import gift.model.Category;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryAcceptanceTest extends AcceptanceTestSupport {

    @BeforeEach
    void setUp() {
        super.setupRestAssured();
    }

    @AfterEach
    void tearDown() {
        super.initAll();
    }

    @Test
    @DisplayName("사용자가 카테고리를 추가한다")
    void createCategory() {
        // Given: 없음

        // When: 카테고리 추가 API를 호출한다
        CreateCategoryRequest request = new CreateCategoryRequest("new beverage");
        Response response = postCategory(request);

        // Then: 상태 코드 200, 응답에 핵심 필드 포함
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo(request.name()));
    }

    private static Response postCategory(CreateCategoryRequest request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/categories");
    }

    @Test
    @DisplayName("사용자가 카테고리를 조회한다")
    void retrieveCategories() {
        // Given: 카테고리가 존재한다
        Category category = super.addCategory("Test category");

        // When: 카테고리 조회 API를 호출한다
        Response response = getCategory();

        // Then: 상태 코드 200, 목록에 카테고리가 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("id", hasItem(category.getId().intValue()))
                .body("name", hasItem(category.getName()));
    }

    private static Response getCategory() {
        return given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/categories");
    }

    @Test
    @DisplayName("사용자가 카테고리를 추가하고 조회한다")
    void createAndRetrieveCategories() {
        // Given: 카테고리가 존재한다
        super.addCategory("Test category");

        // When: 새로운 카테고리를 추가한다
        CreateCategoryRequest request = new CreateCategoryRequest("new beverage 22");

        //noinspection WrapperTypeMayBePrimitive
        Long addedCategoryId = postCategory(request).then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("id");

        // When: 카테고리를 조회한다
        Response response = getCategory();

        // Then: 방금 추가한 카테고리가 목록에 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(addedCategoryId.intValue()))
                .body("name", hasItem(request.name()));
    }
}
