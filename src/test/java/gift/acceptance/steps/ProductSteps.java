package gift.acceptance.steps;

import gift.acceptance.ApiClient;
import gift.acceptance.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext context;

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        ExtractableResponse<Response> response = apiClient.post("/api/categories", Map.of("name", name));
        context.setCategoryId(response.jsonPath().getLong("id"));
    }

    @When("{string} 상품을 가격 {int}원으로 생성하면")
    public void 상품을_생성하면(String name, int price) {
        ExtractableResponse<Response> response = apiClient.post("/api/products",
                Map.of("name", name, "price", price,
                        "imageUrl", "http://image.url", "categoryId", context.getCategoryId()));
        context.setResponse(response);
        if (response.statusCode() == 200) {
            context.setProductId(response.jsonPath().getLong("id"));
        }
    }

    @When("존재하지 않는 카테고리로 상품을 생성하면")
    public void 존재하지_않는_카테고리로_상품을_생성하면() {
        ExtractableResponse<Response> response = apiClient.post("/api/products",
                Map.of("name", "커피", "price", 5000,
                        "imageUrl", "http://image.url", "categoryId", 99999));
        context.setResponse(response);
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        context.setResponse(apiClient.get("/api/products"));
    }

    // HTTP 4xx/5xx 응답을 실패로 판단 (카테고리 미존재: 500)
    @Then("상품 생성에 실패한다")
    public void 상품_생성에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Then("응답에 {string} 상품이 포함되어 있다")
    public void 응답에_상품이_포함되어_있다(String name) {
        String responseName = context.getResponse().jsonPath().getString("name");
        assertThat(responseName).isEqualTo(name);
    }

    @Then("상품 목록에 {string}가 포함되어 있다")
    public void 상품_목록에_이름이_포함되어_있다(String name) {
        List<String> names = context.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }

    @Given("{string} 상품이 가격 {int}원으로 등록되어 있다")
    public void 상품이_등록되어_있다(String name, int price) {
        ExtractableResponse<Response> response = apiClient.post("/api/products",
                Map.of("name", name, "price", price,
                        "imageUrl", "http://image.url", "categoryId", context.getCategoryId()));
        context.setProductId(response.jsonPath().getLong("id"));
    }

    @Then("상품 목록은 비어있다")
    public void 상품_목록은_비어있다() {
        List<?> list = context.getResponse().jsonPath().getList("$");
        assertThat(list).isEmpty();
    }

    @Then("상품 목록에 {int}개가 있다")
    public void 상품_목록에_N개가_있다(int size) {
        List<?> list = context.getResponse().jsonPath().getList("$");
        assertThat(list).hasSize(size);
    }
}
