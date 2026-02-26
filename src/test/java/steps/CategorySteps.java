package steps;

import gift.model.CategoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class CategorySteps {

    private final CategoryRepository categoryRepository;
    private final ScenarioContext context;

    public CategorySteps(ScenarioContext context, CategoryRepository categoryRepository) {
        this.context = context;
        this.categoryRepository = categoryRepository;
    }

    @Given("이름이 {string}인 카테고리가 등록되어 있다")
    public void 카테고리_등록(String name) {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);
    }

    @When("이름이 {string}인 카테고리를 생성하면")
    public void 카테고리_생성(String name) {
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
            .when()
                .post("/api/categories")
        );
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록_조회() {
        context.setResponse(
            given()
            .when()
                .get("/api/categories")
        );
    }

    @Then("DB에 카테고리가 {int}개 저장되어 있다")
    public void DB_카테고리_개수_확인(int count) {
        var categories = categoryRepository.findAll();
        assertThat(categories).hasSize(count);
    }

    @Then("저장된 카테고리의 이름은 {string}이다")
    public void 저장된_카테고리_이름_확인(String name) {
        var categories = categoryRepository.findAll();
        assertThat(categories.get(0).getId()).isNotNull();
        assertThat(categories.get(0).getName()).isEqualTo(name);
    }
}
