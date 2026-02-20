package gift;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @DisplayName("상품을 생성한다")
    @Test
    void 상품을_생성한다() {
        Long categoryId = createCategory("식품").jsonPath().getLong("id");

        var response = createProduct("아메리카노", 4500, "http://example.com/image.png", categoryId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("아메리카노");
        assertThat(response.jsonPath().getInt("price")).isEqualTo(4500);
        assertThat(response.jsonPath().getString("imageUrl")).isEqualTo("http://example.com/image.png");
        assertThat(response.jsonPath().getLong("category.id")).isEqualTo(categoryId);
    }

    @DisplayName("존재하지 않는 카테고리로 상품을 생성하면 실패한다")
    @Test
    void 존재하지_않는_카테고리로_상품을_생성하면_실패한다() {
        var response = createProduct("아메리카노", 4500, "http://example.com/image.png", 999L);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    ExtractableResponse<Response> createProduct(String name, int price, String imageUrl, Long categoryId) {
        return RestAssured.given().log().all()
                .port(port)
                .contentType("application/json")
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products")
                .then().log().all()
                .extract();
    }

    ExtractableResponse<Response> createCategory(String name) {
        return RestAssured.given().log().all()
                .port(port)
                .contentType("application/json")
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then().log().all()
                .extract();
    }
}
