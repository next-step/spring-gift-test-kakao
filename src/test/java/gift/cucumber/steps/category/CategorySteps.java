package gift.cucumber.steps.category;

import gift.cucumber.steps.common.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

public class CategorySteps {
    private final CategoryApiClient categoryApiClient;
    private final CategoryRepositorySupport categoryRepositorySupport;
    private final CategoryResponseAssertions categoryResponseAssertions;
    private final ScenarioContext scenarioContext;

    public CategorySteps(
        final CategoryApiClient categoryApiClient,
        final CategoryRepositorySupport categoryRepositorySupport,
        final CategoryResponseAssertions categoryResponseAssertions,
        final ScenarioContext scenarioContext
    ) {
        this.categoryApiClient = categoryApiClient;
        this.categoryRepositorySupport = categoryRepositorySupport;
        this.categoryResponseAssertions = categoryResponseAssertions;
        this.scenarioContext = scenarioContext;
    }

    @Given("카테고리가 존재하지 않는다")
    public void 카테고리가_존재하지_않는다() {
        categoryRepositorySupport.assertEmpty();
    }

    @Given("다음 카테고리들이 등록되어 있다:")
    public void 다음_카테고리들이_등록되어_있다(DataTable dataTable) {
        final List<String> names = dataTable
            .asMaps(String.class, String.class)
            .stream()
            .map(row -> row.get("name"))
            .toList();
        categoryRepositorySupport.seedCategories(names);
    }

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        var category = categoryRepositorySupport.seedCategory(name);
        scenarioContext.setSavedCategoryId(category.getId());
    }

    @When("관리자가 카테고리 목록을 조회한다")
    public void 관리자가_카테고리_목록을_조회한다() {
        scenarioContext.setLastResponse(categoryApiClient.retrieveCategories());
    }

    @When("관리자가 {string} 카테고리를 생성한다")
    public void 관리자가_카테고리를_생성한다(String name) {
        scenarioContext.setLastResponse(categoryApiClient.createCategory(name));
    }

    @Then("카테고리 목록은 비어있다")
    public void 카테고리_목록은_비어있다() {
        categoryResponseAssertions.assertEmptyList(scenarioContext.getLastResponse());
    }

    @Then("카테고리 목록의 크기는 {int}이다")
    public void 카테고리_목록의_크기는_N이다(int size) {
        categoryResponseAssertions.assertListSize(scenarioContext.getLastResponse(), size);
    }

    @Then("카테고리 목록에 {string} 카테고리가 포함되어 있다")
    public void 카테고리_목록에_카테고리가_포함되어_있다(String name) {
        categoryResponseAssertions.assertContainsCategory(scenarioContext.getLastResponse(), name);
    }

    @Then("응답에 {string} 카테고리 이름이 포함되어 있다")
    public void 응답에_카테고리_이름이_포함되어_있다(String name) {
        categoryResponseAssertions.assertCreatedName(scenarioContext.getLastResponse(), name);
    }

    @Then("데이터베이스에 {string} 카테고리가 저장되어 있다")
    public void 데이터베이스에_카테고리가_저장되어_있다(String name) {
        categoryRepositorySupport.assertSaved(name);
    }
}
