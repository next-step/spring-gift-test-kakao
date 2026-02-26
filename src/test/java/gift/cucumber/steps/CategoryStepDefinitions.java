package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(String name) {
        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories");

        context.set("categoryId:" + name, response.jsonPath().getLong("id"));
    }

    @만일("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories");

        context.set("lastResponse", response);
        context.set("categoryId:" + name, response.jsonPath().getLong("id"));
    }

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        Response response = given()
        .when()
            .get("/api/categories");

        context.set("lastResponse", response);
    }

    @그러면("카테고리 생성이 성공한다")
    public void 카테고리_생성이_성공한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(200);
    }

    @그리고("응답에 {string} 카테고리가 포함되어 있다")
    public void 응답에_카테고리가_포함되어_있다(String name) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("name", equalTo(name));
    }

    @그러면("카테고리 목록에 {int}개가 포함되어 있다")
    public void 카테고리_목록에_N개가_포함되어_있다(int count) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("size()", equalTo(count));
    }
}
