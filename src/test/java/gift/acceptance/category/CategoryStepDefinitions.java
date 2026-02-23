package gift.acceptance.category;

import gift.acceptance.AcceptanceContext;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class CategoryStepDefinitions {

    @Autowired
    private AcceptanceContext context;

    @조건("이름이 {string}인 카테고리가 등록되어 있다")
    public void 이름이_인_카테고리가_등록되어_있다(String name) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200);
    }

    @만약("이름이 {string}인 카테고리 생성을 요청하면")
    public void 이름이_인_카테고리_생성을_요청하면(String name) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .extract();

        context.setResponse(response);
    }

    @만약("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        var response = given()
        .when()
                .get("/api/categories")
        .then()
                .extract();

        context.setResponse(response);
    }
}