package gift;

import gift.model.CategoryRepository;
import gift.model.ProductRepository;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @DisplayName("유효한 상품을 등록한다")
    @Test
    void create_validProduct_returnsCreatedProduct() {
        // Step 1: 카테고리 생성 → categoryId 추출
        int categoryId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "교환권"))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .extract().path("id");

        // Step 2: 상품 등록
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "스타벅스 아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/coffee.jpg",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("스타벅스 아메리카노"))
                .body("price", equalTo(4500))
                .body("imageUrl", equalTo("https://example.com/coffee.jpg"))
                .body("category.id", equalTo(categoryId))
                .body("category.name", equalTo("교환권"));

        // Step 3: 목록 조회로 영속 확인
        RestAssured.given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("스타벅스 아메리카노"));
    }
}
