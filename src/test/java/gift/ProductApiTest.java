package gift;

import gift.model.Category;
import gift.model.Product;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class ProductApiTest extends ApiTest {

    @Test
    @DisplayName("상품을 등록하면 카테고리와 함께 목록에서 조회된다")
    void retrieve_afterSeed_returnsProductWithCategory() {
        // Arrange
        final Category category = categoryRepository.save(new Category("교환권"));
        productRepository.save(new Product("스타벅스 아메리카노", 4500, "https://example.com/image.png", category));

        // Act & Assert
        RestAssured.given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", notNullValue())
                .body("[0].name", equalTo("스타벅스 아메리카노"))
                .body("[0].price", equalTo(4500))
                .body("[0].imageUrl", equalTo("https://example.com/image.png"))
                .body("[0].category.name", equalTo("교환권"));
    }

    @Test
    @DisplayName("상품 목록을 조회하면 등록된 상품이 모두 나타난다")
    void retrieve_multipleProducts_returnsAll() {
        // Arrange
        final Category category = categoryRepository.save(new Category("교환권"));
        productRepository.save(new Product("스타벅스 아메리카노", 4500, "https://example.com/a.png", category));
        productRepository.save(new Product("투썸 케이크", 15000, "https://example.com/b.png", category));

        // Act & Assert
        RestAssured.given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }
}
