package gift.cucumber.steps;

import gift.cucumber.TestContext;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

public class CategorySteps {

    private final TestContext context;

    public CategorySteps(TestContext context) {
        this.context = context;
    }

    @조건("이름이 {string}인 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("name", name))
                .when()
                    .post("/api/categories");

        Long id = response.then().extract().jsonPath().getLong("id");
        context.saveId("category", id);
    }

    @만일("이름이 {string}인 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("name", name))
                .when()
                    .post("/api/categories");

        context.setResponse(response);
    }

    @만일("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        var response = RestAssured
                .given()
                .when()
                    .get("/api/categories");

        context.setResponse(response);
    }

}
