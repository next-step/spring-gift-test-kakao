package gift.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    ScenarioContext scenarioContext;

    @When("^\"([^\"]*)\" 상품을 가격 (\\d+), 이미지 \"([^\"]*)\", 카테고리 (\\d+)로 등록하면$")
    public void 상품을_등록하면(String name, int price, String imageUrl, long categoryId) {
        int statusCode = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": %d
                        }
                        """.formatted(name, price, imageUrl, categoryId))
                .when()
                .post("/api/products")
                .then()
                .extract()
                .statusCode();

        scenarioContext.setResponseStatusCode(statusCode);
    }

    @Then("^상품이 (\\d+)개 등록되어 있다$")
    public void 상품_개수를_확인한다(int expectedCount) {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        assertThat(response.jsonPath().getList("$")).hasSize(expectedCount);
    }

    @Then("^등록된 상품 \"([^\"]*)\"의 카테고리 ID는 (\\d+)이다$")
    public void 상품의_카테고리를_확인한다(String productName, long expectedCategoryId) {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        long categoryId = response.jsonPath()
                .getList("findAll { it.name == '" + productName + "' }.category.id", Long.class)
                .getFirst();

        assertThat(categoryId).isEqualTo(expectedCategoryId);
    }
}
