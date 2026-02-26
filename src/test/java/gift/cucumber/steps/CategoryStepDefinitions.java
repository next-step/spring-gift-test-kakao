package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class CategoryStepDefinitions {

    private final ScenarioContext context;

    public CategoryStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @When("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories");
        context.set("response", response);
    }

    @Given("{string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(String name) {
        Long categoryId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .extract()
                .jsonPath()
                .getLong("id");
        context.set("categoryId", categoryId);
    }

    @When("전체 카테고리를 조회한다")
    public void 전체_카테고리를_조회한다() {
        Response response = given()
        .when()
                .get("/api/categories");
        context.set("response", response);
    }

    @Then("카테고리 생성이 성공한다")
    public void 카테고리_생성이_성공한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Then("응답에 카테고리 이름 {string}가 포함되어 있다")
    public void 응답에_카테고리_이름이_포함되어_있다(String name) {
        Response response = context.get("response", Response.class);
        response.then()
                .body("name", equalTo(name));
    }

    @Then("조회 결과에 {string} 카테고리가 포함되어 있다")
    public void 조회_결과에_카테고리가_포함되어_있다(String name) {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(200)
                .body("name", hasItem(name));
    }
}
