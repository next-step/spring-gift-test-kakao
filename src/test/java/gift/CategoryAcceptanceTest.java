package gift;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // 요청 날렸을 때 url, query param, request body 확인하기 위한 구성품들
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @AfterEach
    void tearDown() {
        // 데이터 초기화
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자가 카테고리를 추가한다")
    void createCategory() {
        // Given: 없음

        // When: 카테고리 추가 API를 호출한다
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "음료"
                ))
                .when()
                .post("/api/categories");

        // Then: 상태 코드 200, 응답에 핵심 필드 포함
        response.then()
                .statusCode(200)
                .body("name", equalTo("음료"));
    }

    @Test
    @DisplayName("사용자가 카테고리를 조회한다")
    void retrieveCategories() {
        // Given: 카테고리가 존재한다
        categoryRepository.save(new Category("테스트 카테고리"));

        // When: 카테고리 조회 API를 호출한다
        var response = RestAssured.given()
                .when()
                .get("/api/categories");

        // Then: 상태 코드 200, 목록에 카테고리가 포함되어 있다
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("사용자가 카테고리를 추가하고 조회한다")
    void createAndRetrieveCategory() {
        // Given: 없음

        // When: 카테고리를 추가한다
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "디저트"
                ))
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200);

        // When: 카테고리를 조회한다
        var response = RestAssured.given()
                .when()
                .get("/api/categories");

        // Then: 방금 추가한 카테고리가 목록에 포함되어 있다
        response.then()
                .statusCode(200)
                .body("name", hasItem("디저트"));
    }
}