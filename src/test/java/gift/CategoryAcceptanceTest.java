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
class CategoryAcceptanceTest {

    private static final String CATEGORY_API_PATH = "/api/categories";

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * C1: 카테고리를 생성하고 목록 조회에서 확인한다.
     */
    @Test
    void 카테고리를_생성하고_목록에서_확인한다() {

        // when — 카테고리 생성
        ExtractableResponse<Response> createResponse = 카테고리를_생성_요청한다("디저트");

        // then
        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("디저트");

        // when — 목록 조회
        ExtractableResponse<Response> listResponse = 카테고리_목록을_조회_요청한다();

        // then
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class))
                .contains("디저트");
    }

    /**
     * C2: 카테고리 목록을 조회한다.
     */
    @Test
    @Sql(
            scripts = {"classpath:cleanup.sql", "classpath:test-data.sql"},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void 카테고리_목록을_조회한다() {

        // when
        ExtractableResponse<Response> response = 카테고리_목록을_조회_요청한다();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
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

    private ExtractableResponse<Response> 카테고리_목록을_조회_요청한다() {
        return RestAssured.given().log().all()
                .when().get(CATEGORY_API_PATH)
                .then().log().all()
                .extract();
    }
}