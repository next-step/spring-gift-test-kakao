package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRetrieveAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.clear();
    }

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
                .body("[0].id", equalTo(product1.getId().intValue()))
                .body("[0].name", equalTo("케이크"))
                .body("[0].price", equalTo(30000))
                .body("[0].imageUrl", equalTo("https://example.com/cake.jpg"))
                .body("[0].category.id", equalTo(category.getId().intValue()))
                .body("[0].category.name", equalTo("식품"))
                .body("[1].id", equalTo(product2.getId().intValue()))
                .body("[1].name", equalTo("초콜릿"))
                .body("[1].price", equalTo(15000))
                .body("[1].category.id", equalTo(category.getId().intValue()));
    }
}
