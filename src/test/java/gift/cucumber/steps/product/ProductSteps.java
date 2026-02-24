package gift.cucumber.steps.product;

import gift.cucumber.steps.common.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;

public class ProductSteps {
    private final ProductApiClient productApiClient;
    private final ProductRepositorySupport productRepositorySupport;
    private final ProductResponseAssertions productResponseAssertions;
    private final ScenarioContext scenarioContext;

    public ProductSteps(
        final ProductApiClient productApiClient,
        final ProductRepositorySupport productRepositorySupport,
        final ProductResponseAssertions productResponseAssertions,
        final ScenarioContext scenarioContext
    ) {
        this.productApiClient = productApiClient;
        this.productRepositorySupport = productRepositorySupport;
        this.productResponseAssertions = productResponseAssertions;
        this.scenarioContext = scenarioContext;
    }

    @Given("상품이 존재하지 않는다")
    public void 상품이_존재하지_않는다() {
        productRepositorySupport.assertEmpty();
    }

    @Given("다음 상품들이 등록되어 있다:")
    public void 다음_상품들이_등록되어_있다(DataTable dataTable) {
        Long categoryId = scenarioContext.getSavedCategoryId();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            productRepositorySupport.seedProduct(
                row.get("name"),
                Integer.parseInt(row.get("price")),
                row.get("imageUrl"),
                categoryId
            );
        }
    }

    @When("관리자가 상품 목록을 조회한다")
    public void 관리자가_상품_목록을_조회한다() {
        scenarioContext.setLastResponse(productApiClient.retrieveProducts());
    }

    @When("관리자가 다음 상품을 생성한다:")
    public void 관리자가_다음_상품을_생성한다(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);
        Long categoryId = scenarioContext.getSavedCategoryId();
        scenarioContext.setLastResponse(productApiClient.createProduct(
            row.get("name"),
            Integer.parseInt(row.get("price")),
            row.get("imageUrl"),
            categoryId
        ));
    }

    @When("관리자가 존재하지 않는 카테고리로 상품을 생성한다:")
    public void 관리자가_존재하지_않는_카테고리로_상품을_생성한다(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);
        scenarioContext.setLastResponse(productApiClient.createProduct(
            row.get("name"),
            Integer.parseInt(row.get("price")),
            row.get("imageUrl"),
            Long.parseLong(row.get("categoryId"))
        ));
    }

    @Then("상품 목록은 비어있다")
    public void 상품_목록은_비어있다() {
        productResponseAssertions.assertEmptyList(scenarioContext.getLastResponse());
    }

    @Then("상품 목록의 크기는 {int}이다")
    public void 상품_목록의_크기는_N이다(int size) {
        productResponseAssertions.assertListSize(scenarioContext.getLastResponse(), size);
    }

    @Then("상품 목록에 {string} 상품이 포함되어 있다")
    public void 상품_목록에_상품이_포함되어_있다(String name) {
        productResponseAssertions.assertContainsProductName(scenarioContext.getLastResponse(), name);
    }

    @Then("응답에 생성된 상품 정보가 포함되어 있다:")
    public void 응답에_생성된_상품_정보가_포함되어_있다(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);
        productResponseAssertions.assertProductFields(
            scenarioContext.getLastResponse(),
            row.get("name"),
            Integer.parseInt(row.get("price")),
            row.get("imageUrl"),
            row.get("categoryName")
        );
    }

    @Then("데이터베이스에 {string} 상품이 저장되어 있다")
    public void 데이터베이스에_상품이_저장되어_있다(String name) {
        productRepositorySupport.assertSavedByName(name);
    }
}
