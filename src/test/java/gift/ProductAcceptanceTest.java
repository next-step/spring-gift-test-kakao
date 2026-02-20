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

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * P1: 상품을 생성하고 목록 조회에서 확인한다.
     * - 카테고리를 API로 먼저 생성 → 해당 카테고리로 상품 생성
     * - POST /api/products → 200 + id, name, price, category 존재
     * - GET /api/products → 방금 생성한 상품이 목록에 포함
     */
    @Test
    void 상품을_생성하고_목록에서_확인한다() {
        // given — 카테고리를 API로 먼저 생성
        ExtractableResponse<Response> categoryResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "간식"))
                .when().post("/api/categories")
                .then().log().all().extract();
        long categoryId = categoryResponse.jsonPath().getLong("id");

        // when — 상품 생성
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "초콜릿",
                        "price", 5000,
                        "imageUrl", "http://img.com/choco.png",
                        "categoryId", categoryId
                ))
                .when().post("/api/products")
                .then().log().all().extract();

        // then — 생성 응답 확인
        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("초콜릿");
        assertThat(createResponse.jsonPath().getInt("price")).isEqualTo(5000);
        assertThat(createResponse.jsonPath().getString("category.name")).isEqualTo("간식");

        // when — 목록 조회로 생성 결과 검증 (시나리오 체이닝)
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract();

        // then — 목록에 방금 생성한 상품 포함
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class)).contains("초콜릿");
    }

    /**
     * P2: 상품 목록을 조회한다.
     * - test-data.sql로 준비된 2건(초콜릿, 커피)이 올바른 카테고리와 함께 조회된다.
     */
    @Test
    @Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void 상품_목록을_조회한다() {
        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
                .containsExactlyInAnyOrder("초콜릿", "커피");
        assertThat(response.jsonPath().getList("category.name", String.class))
                .containsExactlyInAnyOrder("간식", "음료");
    }
}
