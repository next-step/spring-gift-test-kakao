package gift.step;

import static io.restassured.RestAssured.given;

import gift.TestDataManipulator;
import gift.application.request.CreateCategoryRequest;
import gift.model.Category;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

/**
 * 카테고리 관련 Step Definition.
 * <p>
 * {@code 카테고리 "{name}"가 존재한다} step은 product.feature, gift.feature에서도 공유되어 사용된다 (Cucumber의 step은
 * 전역으로 매칭).
 */
public class CategoryStepDefinitions {

    private final AcceptanceContext context;
    private final TestDataManipulator dataManipulator;

    public CategoryStepDefinitions(AcceptanceContext context, TestDataManipulator dataManipulator) {
        this.context = context;
        this.dataManipulator = dataManipulator;
    }

    // --- Given ---

    @Given("카테고리 {string}가 등록되어 있다.")
    public void categoryExists(String name) {
        Category category = dataManipulator.addCategory(name);

        context.putCategoryId(name, category.getId());
    }

    // --- When ---

    @When("카테고리 {string}를 추가한다")
    public void addCategory(String name) {
        CreateCategoryRequest request = new CreateCategoryRequest(name);

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/categories")
        );
    }

    @When("카테고리 목록을 조회한다")
    public void listCategories() {
        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .when()
                        .get("/api/categories")
        );
    }
}
