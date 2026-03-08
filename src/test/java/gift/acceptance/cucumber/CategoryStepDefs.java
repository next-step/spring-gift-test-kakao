package gift.acceptance.cucumber;

import io.cucumber.java.ko.만일;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class CategoryStepDefs {

    @Autowired
    SharedContext context;

    @만일("{string} 이름으로 카테고리를 생성하면")
    public void 이름으로_카테고리를_생성하면(String name) {
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories");
        context.setResponse(response);
    }

    @만일("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        var response = given()
        .when()
            .get("/api/categories");
        context.setResponse(response);
    }
}
