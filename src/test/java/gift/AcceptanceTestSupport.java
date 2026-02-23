package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

class AcceptanceTestSupport {

    static ExtractableResponse<Response> 카테고리를_생성한다(String name) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when().post("/api/categories")
                .then().log().all().extract();
    }
}
