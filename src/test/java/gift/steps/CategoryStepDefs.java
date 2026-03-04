package gift.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class CategoryStepDefs {

    @Autowired
    private ScenarioState state;

    @When("이름이 {string}인 카테고리를 생성한다")
    public void createCategory(final String name) {
        state.setResponse(
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body("{\"name\": \"" + name + "\"}")
                .when()
                        .post("/api/categories")
        );
    }

    @When("카테고리 목록을 조회한다")
    public void retrieveCategories() {
        state.setResponse(
                RestAssured.given()
                .when()
                        .get("/api/categories")
        );
    }

    @Then("응답에 id가 포함되어 있다")
    public void responseContainsId() {
        state.getResponse().then().body("id", notNullValue());
    }

    @Then("응답 목록의 크기는 {int}이다")
    public void responseListSizeIs(final int size) {
        state.getResponse().then().body("$", hasSize(size));
    }
}
