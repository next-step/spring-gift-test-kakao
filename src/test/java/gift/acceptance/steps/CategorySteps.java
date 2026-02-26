package gift.acceptance.steps;

import gift.acceptance.ApiClient;
import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategorySteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext context;

    @When("{string} 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        ExtractableResponse<Response> response = apiClient.post("/api/categories", Map.of("name", name));
        context.setResponse(response);
        if (response.statusCode() == 200) {
            context.setCategoryId(response.jsonPath().getLong("id"));
        }
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        context.setResponse(apiClient.get("/api/categories"));
    }

    @Then("응답에 {string} 카테고리가 포함되어 있다")
    public void 응답에_카테고리가_포함되어_있다(String name) {
        String responseName = context.getResponse().jsonPath().getString("name");
        assertThat(responseName).isEqualTo(name);
    }

    @Then("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_이름이_포함되어_있다(String name) {
        List<String> names = context.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }

    @Then("카테고리 목록은 비어있다")
    public void 카테고리_목록은_비어있다() {
        List<?> list = context.getResponse().jsonPath().getList("$");
        assertThat(list).isEmpty();
    }

    @Then("카테고리 목록에 {int}개가 있다")
    public void 카테고리_목록에_N개가_있다(int size) {
        List<?> list = context.getResponse().jsonPath().getList("$");
        assertThat(list).hasSize(size);
    }
}
