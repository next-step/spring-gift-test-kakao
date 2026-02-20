package gift;

import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiTest {

    @LocalServerPort
    int port;

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Sql("/sql/common-init.sql")
    @Test
    void 상품_등록_성공() {
        RestAssured.given()
            .param("name", "초콜릿")
            .param("price", 10000)
            .param("imageUrl", "img.jpg")
            .param("categoryId", 1L)
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("초콜릿");
        assertThat(products.get(0).getCategory().getId()).isEqualTo(1L);
    }

    @Sql("/sql/common-init.sql")
    @Test
    void 존재하지_않는_카테고리로_상품_등록_시_실패한다() {
        RestAssured.given()
            .param("name", "초콜릿")
            .param("price", 10000)
            .param("imageUrl", "img.jpg")
            .param("categoryId", 999L)
        .when()
            .post("/api/products")
        .then()
            .statusCode(500);

        List<Product> products = productRepository.findAll();
        assertThat(products).isEmpty();
    }
}
