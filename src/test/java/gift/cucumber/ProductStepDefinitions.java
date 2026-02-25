package gift.cucumber;

import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.그러면;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ProductStepDefinitions {

    private static final String DEFAULT_IMAGE_URL = "https://example.com/default.jpg";

    @Autowired
    private SharedContext sharedContext;

    @먼저("{string} 카테고리에 {string} 상품이 가격 {int}으로 등록되어 있다")
    public void 상품이_등록되어_있다(String categoryName, String productName, int price) {
        long categoryId = sharedContext.getId("카테고리_" + categoryName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": %d
                        }
                        """.formatted(productName, price, DEFAULT_IMAGE_URL, categoryId))
                .when()
                .post("/api/products");
        long id = response.then().extract().jsonPath().getLong("id");
        sharedContext.putId("상품_" + productName, id);
    }

    @먼저("{string} 카테고리에 {string} 상품이 등록되어 있다")
    public void 상품이_등록되어_있다(String categoryName, String productName) {
        상품이_등록되어_있다(categoryName, productName, 10000);
    }

    @만일("{string} 카테고리에 {string} 상품을 가격 {int}으로 등록한다")
    public void 상품을_등록한다(String categoryName, String productName, int price) {
        long categoryId = sharedContext.getId("카테고리_" + categoryName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": %d
                        }
                        """.formatted(productName, price, DEFAULT_IMAGE_URL, categoryId))
                .when()
                .post("/api/products");
        sharedContext.setLastResponse(response);
    }

    @만일("존재하지 않는 카테고리에 상품을 등록한다")
    public void 존재하지_않는_카테고리에_상품을_등록한다() {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "아이폰 16",
                            "price": 1500000,
                            "imageUrl": "%s",
                            "categoryId": 999999
                        }
                        """.formatted(DEFAULT_IMAGE_URL))
                .when()
                .post("/api/products");
        sharedContext.setLastResponse(response);
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/products");
        sharedContext.setLastResponse(response);
    }

    @그러면("상품이 정상적으로 등록된다")
    public void 상품이_정상적으로_등록된다() {
        sharedContext.getLastResponse().then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @그러면("상품 등록에 실패한다")
    public void 상품_등록에_실패한다() {
        int statusCode = sharedContext.getLastResponse().then().extract().statusCode();
        assertThat(statusCode).isGreaterThanOrEqualTo(400);
    }

    @그러면("등록된 상품의 이름은 {string}이다")
    public void 등록된_상품의_이름은(String name) {
        sharedContext.getLastResponse().then().body("name", equalTo(name));
    }

    @그러면("등록된 상품의 가격은 {int}이다")
    public void 등록된_상품의_가격은(int price) {
        sharedContext.getLastResponse().then().body("price", equalTo(price));
    }

    @그러면("등록된 상품의 카테고리는 {string}이다")
    public void 등록된_상품의_카테고리는(String categoryName) {
        sharedContext.getLastResponse().then().body("category.name", equalTo(categoryName));
    }

    @그러면("상품 목록에 {string} 가격 {int} 카테고리 {string}이 포함되어 있다")
    public void 상품_목록에_포함(String name, int price, String categoryName) {
        List<Map<String, Object>> products = sharedContext.getLastResponse()
                .then().extract().jsonPath().getList("$");
        assertThat(products).anySatisfy(p -> {
            assertThat(p.get("name")).isEqualTo(name);
            assertThat(p.get("price")).isEqualTo(price);
            assertThat(((Map<?, ?>) p.get("category")).get("name")).isEqualTo(categoryName);
        });
    }
}
