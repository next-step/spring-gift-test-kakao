package gift.acceptance.steps;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        context.setResponse(RestAssured.given()
                .when()
                .get("/api/products"));
    }

    @만일("이름이 {string}이고 가격이 {int}이고 카테고리가 {long}인 상품을 생성한다")
    public void 상품을_생성한다(String name, int price, long categoryId) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", "http://example.com/image.png",
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products"));
    }

    @그리고("상품 목록의 크기는 {int}이다")
    public void 상품_목록의_크기는_이다(int size) {
        context.getResponse().then().body("", hasSize(size));
    }

    @그리고("첫 번째 상품의 이름은 {string}이다")
    public void 첫_번째_상품의_이름은_이다(String name) {
        context.getResponse().then().body("[0].name", equalTo(name));
    }

    @그리고("첫 번째 상품의 가격은 {int}이다")
    public void 첫_번째_상품의_가격은_이다(int price) {
        context.getResponse().then().body("[0].price", equalTo(price));
    }

    @그리고("첫 번째 상품의 카테고리 이름은 {string}이다")
    public void 첫_번째_상품의_카테고리_이름은_이다(String categoryName) {
        context.getResponse().then().body("[0].category.name", equalTo(categoryName));
    }

    @그리고("응답의 상품 이름은 {string}이다")
    public void 응답의_상품_이름은_이다(String name) {
        context.getResponse().then().body("name", equalTo(name));
    }

    @그리고("응답의 상품 가격은 {int}이다")
    public void 응답의_상품_가격은_이다(int price) {
        context.getResponse().then().body("price", equalTo(price));
    }

    @그리고("상품 목록에 {string}가 포함되어 있다")
    public void 상품_목록에_포함되어_있다(String name) {
        context.getResponse().then().body("name", hasItem(name));
    }
}
