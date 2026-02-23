package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

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
    void 정상_상품_등록() {
        var category = categoryRepository.save(new Category("식품"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "아이폰 16",
                            "price": 1500000,
                            "imageUrl": "https://example.com/iphone.jpg",
                            "categoryId": %d
                        }
                        """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("아이폰 16"))
                .body("price", equalTo(1500000))
                .body("imageUrl", equalTo("https://example.com/iphone.jpg"))
                .body("category.id", equalTo(category.getId().intValue()))
                .body("category.name", equalTo("식품"));

        var products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("아이폰 16");
        assertThat(products.get(0).getPrice()).isEqualTo(1500000);
        assertThat(products.get(0).getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void 존재하지_않는_카테고리로_등록_시도() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "아이폰 16",
                            "price": 1500000,
                            "imageUrl": "https://example.com/iphone.jpg",
                            "categoryId": 999999
                        }
                        """)
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        var products = productRepository.findAll();
        assertThat(products).isEmpty();
    }
}
