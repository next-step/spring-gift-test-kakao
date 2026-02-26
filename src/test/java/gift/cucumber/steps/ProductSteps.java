package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ProductSteps {

    @Autowired
    private DataSource dataSource;

    private final ScenarioContext scenarioContext;

    public ProductSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("카테고리와 상품 데이터가 준비되어 있다")
    public void 카테고리와_상품_데이터가_준비되어_있다() {
        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("sql/category-data.sql"));
        populator.execute(dataSource);
    }

    @When("카테고리 {long}에 {string} 상품을 가격 {int}원으로 생성하면")
    public void 카테고리에_상품을_가격으로_생성하면(long categoryId, String name, int price) {
        var response = given()
                .queryParam("name", name)
                .queryParam("price", price)
                .queryParam("imageUrl", "https://example.com/img.jpg")
                .queryParam("categoryId", categoryId)
        .when()
                .post("/api/products");
        scenarioContext.setLastResponse(response);
    }

    @Then("상품 목록에 {string}가 포함되어 있다")
    public void 상품_목록에_가_포함되어_있다(String name) {
        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("[0].name", equalTo(name));
    }

    @Then("상품 목록의 크기가 {int}이다")
    public void 상품_목록의_크기가_이다(int size) {
        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body(".", hasSize(size));
    }
}
