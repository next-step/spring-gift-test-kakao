package gift.acceptance;

import gift.support.ApiClient;
import gift.support.ScenarioContext;
import io.cucumber.java.ko.조건;
import io.cucumber.java.ko.만약;
import io.restassured.response.Response;

public class CategoryStepDefinitions {

	private final ScenarioContext context;
	private final ApiClient apiClient;

	public CategoryStepDefinitions(ScenarioContext context, ApiClient apiClient) {
		this.context = context;
		this.apiClient = apiClient;
	}

	@조건("{string} 카테고리가 등록되어 있다")
	public void 카테고리가_등록되어_있다(String name) {
		Response response = apiClient.createCategory(name);
		Long categoryId = response.then().extract().jsonPath().getLong("id");
		context.set("category:" + name, categoryId);
	}

	@만약("{string} 카테고리를 생성한다")
	public void 카테고리를_생성한다(String name) {
		Response response = apiClient.createCategory(name);
		context.set("response", response);
	}

	@만약("카테고리 목록을 조회한다")
	public void 카테고리_목록을_조회한다() {
		Response response = apiClient.getCategories();
		context.set("response", response);
	}
}
