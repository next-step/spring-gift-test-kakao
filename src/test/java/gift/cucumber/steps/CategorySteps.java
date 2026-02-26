package gift.cucumber.steps;

import static org.hamcrest.Matchers.equalTo;

import gift.cucumber.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class CategorySteps {

    @Autowired
    private TestContext context;

    @When("이름이 {string}인 카테고리를 추가하면")
    public void 카테고리를_추가하면(String name) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .post("/api/categories");
        context.setLastResponse(response);
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        Response response = RestAssured.given()
                .get("/api/categories");
        context.setLastResponse(response);
    }

    @Then("응답에 카테고리명 {string}가 포함되어 있다")
    public void 응답에_카테고리명이_포함되어_있다(String name) {
        context.getLastResponse().then().body("name", equalTo(name));
    }
}
