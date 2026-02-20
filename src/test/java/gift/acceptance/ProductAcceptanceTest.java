package gift.acceptance;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import gift.model.WishRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    void 상품_목록_조회_성공() {
        // given
        var category = categoryRepository.save(new Category("음료"));
        productRepository.save(new Product("아메리카노", 4500, "http://example.com/image.jpg", category));
        productRepository.save(new Product("카페라떼", 5000, "http://example.com/latte.jpg", category));

        // when
        var response = RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name"))
                .containsExactlyInAnyOrder("아메리카노", "카페라떼");
    }

    @Test
    void 상품_없을때_빈_목록을_반환한다() {
        // when
        RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
