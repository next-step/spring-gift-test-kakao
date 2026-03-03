package gift.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class ProductSteps {

    @Autowired
    private SharedContext context;

    @Given("{string} 상품을 가격 {int}, 이미지 {string}로 등록되어 있고")
    public void 상품이_등록되어_있고(String name, int price, String imageUrl) {
        int categoryId = (int) context.getId("categoryId");

        int productId = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(200)
            .extract().path("id");

        context.storeId("productId", productId);
    }

    @When("{string} 상품을 가격 {int}, 이미지 {string}로 등록하면")
    public void 상품을_등록하면(String name, int price, String imageUrl) {
        int categoryId = (int) context.getId("categoryId");

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "name", name,
                    "price", price,
                    "imageUrl", imageUrl,
                    "categoryId", categoryId
                ))
            .when()
                .post("/api/products")
        );
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        context.setResponse(
            given()
            .when()
                .get("/api/products")
        );
    }

    @And("응답의 상품명은 {string}이다")
    public void 응답의_상품명은(String name) {
        context.getResponse()
            .then()
                .body("name", equalTo(name));
    }

    @And("응답의 가격은 {int}이다")
    public void 응답의_가격은(int price) {
        context.getResponse()
            .then()
                .body("price", equalTo(price));
    }

    @And("응답의 이미지는 {string}이다")
    public void 응답의_이미지는(String imageUrl) {
        context.getResponse()
            .then()
                .body("imageUrl", equalTo(imageUrl));
    }

    @And("응답의 카테고리명은 {string}이다")
    public void 응답의_카테고리명은(String categoryName) {
        context.getResponse()
            .then()
                .body("category.name", equalTo(categoryName));
    }

    @And("응답 목록에 상품명 {string}가 포함되어 있다")
    public void 응답_목록에_상품명이_포함되어_있다(String name) {
        context.getResponse()
            .then()
                .body("name", hasItem(name));
    }

    @And("응답 목록의 첫 번째 상품에 카테고리 정보가 포함되어 있다")
    public void 응답_목록의_첫_번째_상품에_카테고리_정보가_포함되어_있다() {
        context.getResponse()
            .then()
                .body("[0].category.id", notNullValue());
    }
}
