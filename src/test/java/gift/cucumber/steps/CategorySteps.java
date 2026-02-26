package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class CategorySteps {

    private final ScenarioContext scenarioContext;

    public CategorySteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("{string} 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        var response = given()
                .queryParam("name", name)
        .when()
                .post("/api/categories");
        scenarioContext.setLastResponse(response);
    }

    @Then("카테고리 목록에 {string}가 포함되어 있다")
    public void 카테고리_목록에_가_포함되어_있다(String name) {
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("name", hasItems(name));
    }

    @Then("카테고리 목록의 크기가 {int}이다")
    public void 카테고리_목록의_크기가_이다(int size) {
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body(".", hasSize(size));
    }

    @Then("카테고리 목록에 {string}, {string}, {string}가 포함되어 있다")
    public void 카테고리_목록에_가_모두_포함되어_있다(String name1, String name2, String name3) {
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("name", hasItems(name1, name2, name3));
    }
}
