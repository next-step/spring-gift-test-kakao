package gift.cucumber.steps;

import gift.cucumber.ScenarioState;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSteps {

    @Autowired
    private ScenarioState state;

    @When("{string} 카테고리에 가격이 {int}원인 {string} 상품을 생성한다")
    public void 상품을_생성한다(String categoryName, int price, String productName) {
        Long categoryId = state.getCategoryId(categoryName);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", productName,
                        "price", price,
                        "imageUrl", "http://img.com/" + productName + ".png",
                        "categoryId", categoryId
                ))
                .when().post("/api/products")
                .then().log().all().extract();
        state.setLastResponse(response);
        if (response.statusCode() == 200) {
            state.putProductId(productName, response.jsonPath().getLong("id"));
        }
    }

    @Then("상품이 정상적으로 생성된다")
    public void 상품이_정상적으로_생성된다() {
        ExtractableResponse<Response> response = state.getLastResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getLong("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isNotBlank();
    }

    @Then("상품의 카테고리가 {string}이다")
    public void 상품의_카테고리가_이다(String categoryName) {
        ExtractableResponse<Response> response = state.getLastResponse();
        assertThat(response.jsonPath().getString("category.name")).isEqualTo(categoryName);
    }

    @Then("상품 목록을 조회하면 {string}이 포함되어 있다")
    public void 상품_목록을_조회하면_포함되어_있다(String productName) {
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract();

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.jsonPath().getList("name", String.class)).contains(productName);
    }

    @Given("{string} 카테고리에 가격이 {int}원인 {string} 상품이 등록되어 있다")
    public void 상품이_등록되어_있다(String categoryName, int price, String productName) {
        if (state.getProductId(productName) == null) {
            상품을_생성한다(categoryName, price, productName);
        }
    }

    @When("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        state.setLastResponse(RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract());
    }

    @Then("상품 목록에 {string}, {string}가 포함되어 있다")
    public void 상품_목록에_포함되어_있다(String name1, String name2) {
        ExtractableResponse<Response> response = state.getLastResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("name", String.class))
                .contains(name1, name2);
    }
}
