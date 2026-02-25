package gift.acceptance;

import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import gift.model.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

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
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    void 상품_생성_성공() {
        // given
        var categoryId = 카테고리_생성("음료").jsonPath().getLong("id");

        // when
        var response = 상품_생성("아메리카노", 4500, "http://example.com/image.jpg", categoryId);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("아메리카노");
        assertThat(response.jsonPath().getInt("price")).isEqualTo(4500);
    }

    @Test
    void 존재하지_않는_카테고리로_상품_생성시_실패한다() {
        // when
        var response = 상품_생성("아메리카노", 4500, "http://example.com/image.jpg", Long.MAX_VALUE);

        // then
        assertThat(response.statusCode()).isEqualTo(500);
    }

    @Test
    void 상품_목록_조회_성공() {
        // given
        var categoryId = 카테고리_생성("음료").jsonPath().getLong("id");
        assertThat(상품_생성("아메리카노", 4500, "http://example.com/image.jpg", categoryId).statusCode()).isEqualTo(200);
        assertThat(상품_생성("카페라떼", 5000, "http://example.com/latte.jpg", categoryId).statusCode()).isEqualTo(200);

        // when
        var response = RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(response.jsonPath().getList("id")).doesNotContainNull();
        assertThat(response.jsonPath().getList("name"))
                .containsExactlyInAnyOrder("아메리카노", "카페라떼");
        assertThat(response.jsonPath().getList("price"))
                .containsExactlyInAnyOrder(4500, 5000);
        assertThat(response.jsonPath().getList("imageUrl")).doesNotContainNull();
        assertThat(response.jsonPath().getList("category")).doesNotContainNull();
    }

    private ExtractableResponse<Response> 카테고리_생성(String name) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then()
                .extract();
    }

    private ExtractableResponse<Response> 상품_생성(String name, int price, String imageUrl, Long categoryId) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products")
                .then()
                .extract();
    }

    @Test
    void 상품_없을때_빈_목록을_반환한다() {
        // when
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
