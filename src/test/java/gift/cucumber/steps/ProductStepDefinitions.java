package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class ProductStepDefinitions {

    @LocalServerPort
    int port;

    private final ScenarioContext context;

    public ProductStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @When("{string} 상품을 가격 {int}원으로 생성한다")
    public void 상품을_가격으로_생성한다(String name, int price) {
        RestAssured.port = port;
        Long categoryId = context.get("categoryId", Long.class);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products");
        context.set("response", response);
    }

    @Given("{string} 상품이 존재한다")
    public void 상품이_존재한다(String name) {
        RestAssured.port = port;
        Long categoryId = context.get("categoryId", Long.class);
        Long productId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .extract()
                .jsonPath()
                .getLong("id");
        context.set("productId", productId);
    }

    @When("전체 상품을 조회한다")
    public void 전체_상품을_조회한다() {
        RestAssured.port = port;
        Response response = given()
        .when()
                .get("/api/products");
        context.set("response", response);
    }

    @When("존재하지 않는 카테고리로 상품을 생성한다")
    public void 존재하지_않는_카테고리로_상품을_생성한다() {
        RestAssured.port = port;
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", 999999
                ))
        .when()
                .post("/api/products");
        context.set("response", response);
    }

    @Then("상품 생성이 성공한다")
    public void 상품_생성이_성공한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Then("상품 생성이 실패한다")
    public void 상품_생성이_실패한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(500);
    }

    @Then("응답에 상품 이름 {string}가 포함되어 있다")
    public void 응답에_상품_이름이_포함되어_있다(String name) {
        Response response = context.get("response", Response.class);
        response.then()
                .body("name", equalTo(name));
    }

    @Then("조회 결과에 {string} 상품이 포함되어 있다")
    public void 조회_결과에_상품이_포함되어_있다(String name) {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(200)
                .body("name", hasItem(name));
    }
}
