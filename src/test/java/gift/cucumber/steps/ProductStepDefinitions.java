package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 카테고리에 {int}원짜리 {string} 상품이 존재한다")
    public void 상품이_존재한다(String categoryName, int price, String productName) {
        Long categoryId = context.get("categoryId:" + categoryName, Long.class);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", productName,
                "price", price,
                "imageUrl", "https://example.com/image.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");

        context.set("productId:" + productName, response.jsonPath().getLong("id"));
    }

    @만일("{string} 카테고리에 {int}원짜리 {string} 상품을 등록한다")
    public void 상품을_등록한다(String categoryName, int price, String productName) {
        Long categoryId = context.get("categoryId:" + categoryName, Long.class);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", productName,
                "price", price,
                "imageUrl", "https://example.com/image.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");

        context.set("lastResponse", response);
        context.set("productId:" + productName, response.jsonPath().getLong("id"));
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        Response response = given()
        .when()
            .get("/api/products");

        context.set("lastResponse", response);
    }

    @그러면("상품 등록이 성공한다")
    public void 상품_등록이_성공한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(200);
    }

    @그리고("응답에 {string} 상품이 포함되어 있다")
    public void 응답에_상품이_포함되어_있다(String productName) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("name", equalTo(productName));
    }

    @그리고("응답에 카테고리 {string}이 포함되어 있다")
    public void 응답에_카테고리가_포함되어_있다(String categoryName) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("category.name", equalTo(categoryName));
    }

    @그러면("상품 목록에 {int}개가 포함되어 있다")
    public void 상품_목록에_N개가_포함되어_있다(int count) {
        Response response = context.get("lastResponse", Response.class);
        response.then().body("size()", equalTo(count));
    }
}
