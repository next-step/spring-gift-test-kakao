package gift.cucumber.steps;

import gift.cucumber.TestContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class ProductSteps {

    private final TestContext context;

    public ProductSteps(TestContext context) {
        this.context = context;
    }

    @먼저("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String categoryName) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("name", categoryName))
                .when()
                    .post("/api/categories");

        Long id = response.then().extract().jsonPath().getLong("id");
        context.saveId("category", id);
    }

    @그리고("이름이 {string}, 가격이 {int}, 이미지가 {string}인 상품이 등록되어 있다")
    public void 상품이_등록되어_있다(String name, int price, String imageUrl) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "name", name,
                            "price", price,
                            "imageUrl", imageUrl,
                            "categoryId", context.getId("category")
                    ))
                .when()
                    .post("/api/products");

        Long id = response.then().extract().jsonPath().getLong("id");
        context.saveId("product", id);
    }

    @만일("이름이 {string}, 가격이 {int}, 이미지가 {string}인 상품을 등록한다")
    public void 상품을_등록한다(String name, int price, String imageUrl) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "name", name,
                            "price", price,
                            "imageUrl", imageUrl,
                            "categoryId", context.getId("category")
                    ))
                .when()
                    .post("/api/products");

        context.setResponse(response);
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        var response = RestAssured
                .given()
                .when()
                    .get("/api/products");

        context.setResponse(response);
    }

    @만일("존재하지 않는 카테고리로 이름이 {string}, 가격이 {int}, 이미지가 {string}인 상품을 등록한다")
    public void 존재하지_않는_카테고리로_상품을_등록한다(String name, int price, String imageUrl) {
        var response = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "name", name,
                            "price", price,
                            "imageUrl", imageUrl,
                            "categoryId", 999
                    ))
                .when()
                    .post("/api/products");

        context.setResponse(response);
    }

    @그리고("응답 본문의 상품 이름은 {string}이다")
    public void 응답_본문의_상품_이름_검증(String name) {
        context.getResponse().then().body("name", equalTo(name));
    }

    @그리고("응답 본문의 상품 가격은 {int}이다")
    public void 응답_본문의_상품_가격_검증(int price) {
        context.getResponse().then().body("price", equalTo(price));
    }

    @그리고("응답 본문의 상품 목록에 {string}가 포함되어 있다")
    public void 응답_본문의_상품_목록에_포함(String name) {
        context.getResponse().then().body("name", hasItem(name));
    }
}
