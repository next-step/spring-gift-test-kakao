package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String categoryName) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + categoryName + "\"}")
                .when()
                .post("/api/categories");

        context.set("lastResponse", response);
        context.set("categoryName", categoryName);
    }

    @그러면("카테고리 생성이 성공한다")
    public void 카테고리_생성이_성공한다() {
        Response response = context.get("lastResponse", Response.class);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @그리고("응답에 {string} 카테고리 이름이 포함되어 있다")
    public void 응답에_카테고리_이름이_포함되어_있다(String categoryName) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("name", equalTo(categoryName));
    }

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        Response response = RestAssured.given()
                .when()
                .get("/api/categories");

        context.set("lastResponse", response);
    }

    @그러면("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_포함되어_있다(String categoryName) {
        Response response = context.get("lastResponse", Response.class);
        response.then()
                .statusCode(200)
                .body("name", hasItem(categoryName));
    }
}
