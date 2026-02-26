package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.Map;

public class ApiClient {

    private final int port;

    public ApiClient(int port) {
        this.port = port;
    }

    public ExtractableResponse<Response> post(String path, Map<String, Object> body) {
        return RestAssured.given()
                .port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
            .when()
                .post(path)
            .then()
                .extract();
    }

    public ExtractableResponse<Response> post(String path, Map<String, Object> body, Map<String, Object> headers) {
        var request = RestAssured.given()
                .port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body);
        headers.forEach((key, value) -> request.header(key, value));
        return request
            .when()
                .post(path)
            .then()
                .extract();
    }

    public ExtractableResponse<Response> get(String path) {
        return RestAssured.given()
                .port(port)
            .when()
                .get(path)
            .then()
                .extract();
    }
}
