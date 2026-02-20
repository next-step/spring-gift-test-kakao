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
