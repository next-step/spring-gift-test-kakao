package gift.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CategorySteps {

    @Autowired
    private SharedContext context;

    @Given("이름이 {string}인 카테고리가 등록되어 있고")
    public void 카테고리가_등록되어_있고(String name) {
        int categoryId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .extract().path("id");

        context.storeId("categoryId", categoryId);
    }

    @When("이름이 {string}인 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
            .when()
                .post("/api/categories")
        );
    }

    @When("이름 없이 카테고리를 생성하면")
    public void 이름_없이_카테고리를_생성하면() {
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of())
            .when()
                .post("/api/categories")
        );
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        context.setResponse(
            given()
            .when()
                .get("/api/categories")
        );
    }

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는(int statusCode) {
        context.getResponse()
            .then()
                .statusCode(statusCode);
    }

    @And("응답의 id는 비어있지 않다")
    public void 응답의_id는_비어있지_않다() {
        context.getResponse()
            .then()
                .body("id", notNullValue());
    }

    @And("응답의 name은 {string}이다")
    public void 응답의_name은(String name) {
        context.getResponse()
            .then()
                .body("name", equalTo(name));
    }

    @And("응답의 name은 null이다")
    public void 응답의_name은_null이다() {
        context.getResponse()
            .then()
                .body("name", nullValue());
    }

    @And("응답 목록에 이름이 {string}인 카테고리가 포함되어 있다")
    public void 응답_목록에_카테고리가_포함되어_있다(String name) {
        context.getResponse()
            .then()
                .body("name", hasItem(name));
    }

    @And("응답 목록의 크기는 {int}이다")
    public void 응답_목록의_크기는(int size) {
        context.getResponse()
            .then()
                .body("size()", equalTo(size));
    }
}
