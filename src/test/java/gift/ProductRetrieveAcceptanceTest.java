package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

class ProductRetrieveAcceptanceTest extends AcceptanceTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void 데이터가_없을_때_빈_목록_반환() {
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void 등록한_상품이_목록에_포함_카테고리_중첩_응답() {
        var category = categoryRepository.save(new Category("식품"));
        var product1 = productRepository.save(new Product("케이크", 30000, "https://example.com/cake.jpg", category));
        var product2 = productRepository.save(new Product("초콜릿", 15000, "https://example.com/choco.jpg", category));

        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("케이크", "초콜릿"))
                .body("category.name", hasItems("식품"));
    }
}
