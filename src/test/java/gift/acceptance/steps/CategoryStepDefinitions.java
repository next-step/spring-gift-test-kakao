package gift.acceptance.steps;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        context.setResponse(RestAssured.given()
                .when()
                .get("/api/categories"));
    }

    @만일("이름이 {string}인 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories"));
    }

    @그리고("카테고리 목록의 크기는 {int}이다")
    public void 카테고리_목록의_크기는_이다(int size) {
        context.getResponse().then().body("", hasSize(size));
    }

    @그리고("첫 번째 카테고리의 이름은 {string}이다")
    public void 첫_번째_카테고리의_이름은_이다(String name) {
        context.getResponse().then().body("[0].name", equalTo(name));
    }

    @그리고("응답의 카테고리 이름은 {string}이다")
    public void 응답의_카테고리_이름은_이다(String name) {
        context.getResponse().then().body("name", equalTo(name));
    }

    @그리고("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_포함되어_있다(String name) {
        context.getResponse().then().body("name", hasItem(name));
    }
}
