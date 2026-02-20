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

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * C1: 카테고리를 생성하고 목록 조회에서 확인한다.
     * - cleanup 후 빈 상태에서 시작 → ID 충돌 없음
     * - POST /api/categories → 200 + id, name 존재
     * - GET /api/categories → 방금 생성한 카테고리가 목록에 포함
     */
    @Test
    void 카테고리를_생성하고_목록에서_확인한다() {
        // when — 카테고리 생성
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "디저트"))
                .when().post("/api/categories")
                .then().log().all().extract();

        // then — 생성 응답 확인
        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(createResponse.jsonPath().getLong("id")).isNotNull();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("디저트");

        // when — 목록 조회로 생성 결과 검증 (시나리오 체이닝)
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when().get("/api/categories")
                .then().log().all().extract();

        // then — 목록에 방금 생성한 카테고리 포함
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class)).contains("디저트");
    }

    /**
     * C2: 카테고리 목록을 조회한다.
     * - test-data.sql로 준비된 2건(간식, 음료)이 조회된다.
     */
    @Test
    @Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void 카테고리_목록을_조회한다() {
        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/api/categories")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
                .containsExactlyInAnyOrder("간식", "음료");
    }
}
