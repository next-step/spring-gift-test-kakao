package gift.steps;

import gift.application.CreateProductRequest;
import gift.fixture.ProductFixture;
import gift.model.CategoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    CategoryRepository categoryRepository;

    private ProductFixture fixture;
    private ExtractableResponse<Response> listResponse;
    private Response response;

    @Given("{string} 카테고리가 준비되어 있다")
    public void 카테고리가_준비되어_있다(String categoryName) {
        fixture = ProductFixture.builder(categoryRepository)
                .category(categoryName)
                .build();
    }

    @When("{string} 상품을 생성한다")
    public void 상품을_생성한다(String productName) {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(new CreateProductRequest(productName, 4500, "https://example.com/image.png", fixture.categoryId()))
                .when()
                .post("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract();
    }

    @When("존재하지 않는 카테고리로 {string} 상품을 생성하면")
    public void 존재하지_않는_카테고리로_상품을_생성하면(String productName) {
        Long nonExistentCategoryId = fixture.categoryId() + 1;

        response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(new CreateProductRequest(productName, 4500, "https://example.com/image.png", nonExistentCategoryId))
                .when()
                .post("/api/products")
                .then().log().all()
                .extract().response();
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        listResponse = RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract();
    }

    @Then("상품 목록에 {string} 상품이 포함되어 있다")
    public void 상품_목록에_상품이_포함되어_있다(String productName) {
        List<String> names = listResponse.jsonPath().getList("name", String.class);
        assertThat(names).contains(productName);
    }

    @Then("상품 생성에 실패한다")
    public void 상품_생성에_실패한다() {
        assertThat(response.jsonPath().getString("error")).isNotNull();
    }
}
