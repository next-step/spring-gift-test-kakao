package gift.cucumber.steps;

import static org.hamcrest.Matchers.equalTo;

import gift.cucumber.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class ProductSteps {

    @Autowired
    private TestContext context;

    @When("이름 {string}, 가격 {int}원, 이미지 {string}인 상품을 추가하면")
    public void 상품을_추가하면(String name, int price, String imageUrl) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", context.getCategoryId()
                ))
                .post("/api/products");
        context.setLastResponse(response);
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        Response response = RestAssured.given()
                .get("/api/products");
        context.setLastResponse(response);
    }

    @When("존재하지 않는 카테고리 ID로 상품을 추가하면")
    public void 존재하지_않는_카테고리_ID로_상품을_추가하면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "에러 상품",
                        "price", 1000,
                        "imageUrl", "https://example.com/error.jpg",
                        "categoryId", 999999L
                ))
                .post("/api/products");
        context.setLastResponse(response);
    }

    @When("카테고리 ID 없이 상품을 추가하면")
    public void 카테고리_ID_없이_상품을_추가하면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "에러 상품",
                        "price", 1000,
                        "imageUrl", "https://example.com/error.jpg"
                ))
                .post("/api/products");
        context.setLastResponse(response);
    }

    @When("가격이 {int}원인 상품을 추가하면")
    public void 가격이_음수인_상품을_추가하면(int price) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "음수 가격 상품",
                        "price", price,
                        "imageUrl", "https://example.com/negative.jpg",
                        "categoryId", context.getCategoryId()
                ))
                .post("/api/products");
        context.setLastResponse(response);
    }

    @Then("응답에 상품명 {string}가 포함되어 있다")
    public void 응답에_상품명이_포함되어_있다(String name) {
        context.getLastResponse().then().body("name", equalTo(name));
    }
}
