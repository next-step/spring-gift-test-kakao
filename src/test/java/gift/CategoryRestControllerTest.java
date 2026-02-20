package gift;

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

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CategoryRestControllerTest {

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
    void create_validName_returnsCreatedCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("식품"));
    }

    @Test
    @DisplayName("생성된 카테고리는 목록 조회 시 포함된다")
    void create_thenRetrieve_containsCreatedCategory() {
        // given
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        // when & then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem("식품"));
    }

    @Test
    @DisplayName("빈 이름으로 카테고리를 생성하면 빈 이름으로 저장된다")
    void create_emptyName_savedWithEmptyName() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", ""))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo(""));
    }

    @Test
    @DisplayName("name 필드 누락 시 null로 저장된다")
    void create_missingName_savedWithNull() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of())
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", nullValue());
    }

    @Test
    @DisplayName("동일한 이름의 카테고리를 여러 개 생성할 수 있다")
    void create_duplicateName_allowsMultiple() {
        // given
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        // when
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        // then
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }
}
