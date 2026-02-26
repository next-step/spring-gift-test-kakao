package gift.legacy;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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

    private Category category;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        category = categoryRepository.save(new Category("교환권"));
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("유효한 상품 정보로 등록하면 200 OK와 생성된 상품을 반환한다")
    void createValidProductReturnsCreatedProduct() {
        createProduct("스타벅스 아메리카노", 4500, "https://example.com/coffee.jpg")
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("스타벅스 아메리카노"))
            .body("price", equalTo(4500))
            .body("imageUrl", equalTo("https://example.com/coffee.jpg"))
            .body("category.id", notNullValue())
            .body("category.name", equalTo("교환권"));
    }

    @Test
    @DisplayName("상품이 존재할 때 목록을 조회하면 전체 상품 목록을 반환한다")
    void retrieveProductsReturnsList() {
        // given
        createProduct("스타벅스 아메리카노", 4500, "https://example.com/americano.jpg").statusCode(200);
        createProduct("스타벅스 카페라떼", 5000, "https://example.com/latte.jpg").statusCode(200);

        // when & then
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

    private ValidatableResponse createProduct(String name, int price, String imageUrl) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", category.getId()
            ))
        .when()
            .post("/api/products")
        .then();
    }
}
