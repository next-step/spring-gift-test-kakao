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
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @DisplayName("카테고리를 생성한다")
    @Test
    void 카테고리를_생성한다() {
        var response = createCategory("식품");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("식품");
    }

    @DisplayName("카테고리를 전체 조회한다")
    @Test
    void 카테고리를_전체_조회한다() {
        createCategory("식품");
        createCategory("패션");

        var response = retrieveCategories();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("name")).contains("식품", "패션");
    }

    @DisplayName("카테고리가 없으면 빈 리스트를 반환한다")
    @Test
    void 카테고리가_없으면_빈_리스트를_반환한다() {
        var response = retrieveCategories();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("$")).isEmpty();
    }

    ExtractableResponse<Response> retrieveCategories() {
        return RestAssured.given().log().all()
                .port(port)
                .when()
                .get("/api/categories")
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
