package gift.steps;

import gift.application.CreateProductRequest;
import gift.fixture.ProductFixture;
import gift.model.CategoryRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Value("${app.port}")
    int appPort;

    @Autowired
    CategoryRepository categoryRepository;

    private ProductFixture fixture;
    private Long createdProductId;
    private Response response;

    @Before
    public void setUp() {
        RestAssured.port = appPort;
    }

    @Given("{string} 카테고리가 준비되어 있다")
    public void 카테고리가_준비되어_있다(String categoryName) {
        fixture = ProductFixture.builder(categoryRepository)
                .category(categoryName)
                .build();
    }

    @When("{string} 상품을 생성한다")
    public void 상품을_생성한다(String productName) {
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(new CreateProductRequest(productName, 4500, "https://example.com/image.png", fixture.categoryId()))
                .when()
                .post("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract();

        createdProductId = createResponse.jsonPath().getLong("id");
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

    @Then("상품 목록을 조회하면 {string} 상품이 포함되어 있다")
    public void 상품_목록을_조회하면_상품이_포함되어_있다(String productName) {
        ExtractableResponse<Response> listResponse = RestAssured.given().log().all()
                .when()
                .get("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract();

        List<String> names = listResponse.jsonPath().getList("name", String.class);
        assertThat(names).contains(productName);
    }

    @Then("상품 생성 응답 상태 코드는 {int}이다")
    public void 상품_생성_응답_상태_코드는_N이다(int statusCode) {
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }
}
