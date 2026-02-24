package gift.acceptance;

import gift.support.ApiClient;
import gift.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

public class ProductStepDefinitions {

	private final ScenarioContext context;
	private final ApiClient apiClient;

	public ProductStepDefinitions(ScenarioContext context, ApiClient apiClient) {
		this.context = context;
		this.apiClient = apiClient;
	}

	@Given("{string} 카테고리에 {string} 상품이 등록되어 있다")
	public void 상품이_등록되어_있다(String categoryName, String productName) {
		Long categoryId = context.get("category:" + categoryName, Long.class);
		apiClient.createProduct(productName, 10000, categoryId);
	}

	@When("{string} 카테고리에 {string} 상품을 {int}원으로 생성한다")
	public void 상품을_생성한다(String categoryName, String productName, int price) {
		Long categoryId = context.get("category:" + categoryName, Long.class);
		Response response = apiClient.createProduct(productName, price, categoryId);
		context.set("response", response);
	}

	@When("상품 목록을 조회한다")
	public void 상품_목록을_조회한다() {
		Response response = apiClient.getProducts();
		context.set("response", response);
	}

	@When("존재하지 않는 카테고리에 상품을 생성한다")
	public void 존재하지_않는_카테고리에_상품을_생성한다() {
		Response response = apiClient.createProduct("맥북 에어", 1500000, 999L);
		context.set("response", response);
	}
}
