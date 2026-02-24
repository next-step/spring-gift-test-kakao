package gift.cucumber.steps;

import gift.cucumber.ScenarioState;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategorySteps {

    @Autowired
    private ScenarioState state;

    @When("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when().post("/api/categories")
                .then().log().all().extract();
        state.setLastResponse(response);
        if (response.statusCode() == 200) {
            state.putCategoryId(name, response.jsonPath().getLong("id"));
        }
    }

    @Then("카테고리가 정상적으로 생성된다")
    public void 카테고리가_정상적으로_생성된다() {
        ExtractableResponse<Response> response = state.getLastResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isNotBlank();
    }

    @Then("카테고리 목록을 조회하면 {string}가 포함되어 있다")
    public void 카테고리_목록을_조회하면_포함되어_있다(String name) {
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when().get("/api/categories")
                .then().log().all().extract();

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class)).contains(name);
    }

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        if (state.getCategoryId(name) == null) {
            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(Map.of("name", name))
                    .when().post("/api/categories")
                    .then().log().all().extract();
            state.putCategoryId(name, response.jsonPath().getLong("id"));
        }
    }

    @When("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        state.setLastResponse(RestAssured.given().log().all()
                .when().get("/api/categories")
                .then().log().all().extract());
    }

    @Then("카테고리 목록에 {string}, {string}가 포함되어 있다")
    public void 카테고리_목록에_포함되어_있다(String name1, String name2) {
        ExtractableResponse<Response> response = state.getLastResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
                .contains(name1, name2);
    }
}
