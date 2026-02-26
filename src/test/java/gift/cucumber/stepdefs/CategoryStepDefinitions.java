package gift.cucumber.stepdefs;

import gift.cucumber.ScenarioContext;
import gift.model.Category;
import gift.model.CategoryRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @When("이름이 {string}인 카테고리를 생성한다")
    public void 이름이_N인_카테고리를_생성한다(String name) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "%s"}
                    """.formatted(name))
            .when()
                .post("/api/categories")
        );
    }

    @And("카테고리 {string}가 저장되어 있다")
    public void 카테고리가_저장되어_있다(String name) {
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).anyMatch(c -> c.getName().equals(name));
    }
}
