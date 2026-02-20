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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductRestControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("유효한 상품을 등록한다")
    void create_validProduct_returnsCreatedProduct() {
        // given: 카테고리 생성
        int categoryId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "교환권"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .extract().path("id");

        // when & then: 상품 등록
        given()
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
    }

    @Test
    @DisplayName("상품 목록을 조회한다")
    void retrieve_products_returnsList() {
        // given: 카테고리 1개 + 상품 2개 생성
        int categoryId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "교환권"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "스타벅스 아메리카노",
                "price", 4500,
                "imageUrl", "https://example.com/americano.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "스타벅스 카페라떼",
                "price", 5000,
                "imageUrl", "https://example.com/latte.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);

        // when & then: 목록 조회
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("name", hasItem("스타벅스 아메리카노"))
            .body("name", hasItem("스타벅스 카페라떼"))
            .body("[0].category.id", notNullValue());
    }
}
