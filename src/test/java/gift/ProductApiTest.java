package gift;

import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 상품_등록_성공() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "초콜릿",
                    "price": 10000,
                    "imageUrl": "img.jpg",
                    "categoryId": 1
                }
                """)
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("초콜릿");
        assertThat(products.get(0).getCategory().getId()).isEqualTo(1L);
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 가격에_잘못된_타입을_보내면_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "초콜릿",
                    "price": "만원",
                    "imageUrl": "img.jpg",
                    "categoryId": 1
                }
                """)
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);

        List<Product> products = productRepository.findAll();
        assertThat(products).isEmpty();
    }

    @Sql({"/sql/h2/cleanup.sql", "/sql/common-data.sql", "/sql/h2/reset-sequences.sql"})
    @Test
    void 존재하지_않는_카테고리로_상품_등록_시_실패한다() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "초콜릿",
                    "price": 10000,
                    "imageUrl": "img.jpg",
                    "categoryId": 999
                }
                """)
        .when()
            .post("/api/products")
        .then()
            .statusCode(500);

        List<Product> products = productRepository.findAll();
        assertThat(products).isEmpty();
    }
}
