package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductAcceptanceTest {

    private static final String CATEGORY_API_PATH = "/api/categories";
    private static final String PRODUCT_API_PATH = "/api/products";

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * P1: 상품을 생성하고 목록 조회에서 확인한다.
     */
    @Test
    void 상품을_생성하고_목록에서_확인한다() {
        // given — 카테고리를 API로 먼저 생성
        ExtractableResponse<Response> categoryResponse = 카테고리를_생성_요청한다("간식");
        long categoryId = categoryResponse.jsonPath().getLong("id");

        // when — 상품 생성
        ExtractableResponse<Response> createResponse = 상품을_생성_요청한다(
                "초콜릿",
                5000,
                "http://img.com/choco.png",
                categoryId
        );

        // then — 생성 응답 확인 (assert 그대로)
        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("초콜릿");
        assertThat(createResponse.jsonPath().getInt("price")).isEqualTo(5000);
        assertThat(createResponse.jsonPath().getString("category.name")).isEqualTo("간식");

        // when — 목록 조회로 생성 결과 검증
        ExtractableResponse<Response> listResponse = 상품_목록을_조회_요청한다();

        // then — 목록에 방금 생성한 상품 포함
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class)).contains("초콜릿");
    }

    /**
     * P2: 상품 목록을 조회한다.
     */
    @Test
    @Sql(
            scripts = {"classpath:cleanup.sql", "classpath:test-data.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void 상품_목록을_조회한다() {
        // when
        ExtractableResponse<Response> response = 상품_목록을_조회_요청한다();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
                .containsExactlyInAnyOrder("초콜릿", "커피");
        assertThat(response.jsonPath().getList("category.name", String.class))
                .containsExactlyInAnyOrder("간식", "음료");
    }

    // =========================
    // Helper Methods (HTTP 호출만 추출)
    // =========================

    private ExtractableResponse<Response> 카테고리를_생성_요청한다(String name) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when().post(CATEGORY_API_PATH)
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 상품을_생성_요청한다(String name, int price, String imageUrl, long categoryId) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
                .when().post(PRODUCT_API_PATH)
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 상품_목록을_조회_요청한다() {
        return RestAssured.given().log().all()
                .when().get(PRODUCT_API_PATH)
                .then().log().all()
                .extract();
    }
}
