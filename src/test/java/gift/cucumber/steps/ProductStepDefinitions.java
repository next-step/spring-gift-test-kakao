package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.Category;
import gift.model.ProductRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static gift.Fixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

public class ProductStepDefinitions {

    @Autowired
    ScenarioContext context;

    @Autowired
    ProductRepository productRepository;

    @만일("{string} 카테고리에 {string} 상품을 등록한다")
    public void 카테고리에_상품을_등록한다(String categoryName, String productName) {
        Category category = context.get("category:" + categoryName);

        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": 10000,
                            "imageUrl": "https://example.com/default.jpg",
                            "categoryId": %d
                        }
                        """.formatted(productName, category.getId()))
                .when()
                .post("/api/products");

        context.setLastResponse(response);
    }

    @만일("존재하지 않는 카테고리에 {string} 상품을 등록한다")
    public void 존재하지_않는_카테고리에_상품_등록(String productName) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": 10000,
                            "imageUrl": "https://example.com/default.jpg",
                            "categoryId": 999999
                        }
                        """.formatted(productName))
                .when()
                .post("/api/products");

        context.setLastResponse(response);
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/products");

        context.setLastResponse(response);
    }

    @그리고("응답에 상품 이름 {string}이 포함되어 있다")
    public void 응답에_상품_이름이_포함(String name) {
        context.getLastResponse().then()
                .body("name", equalTo(name));
    }

    @그리고("데이터베이스에 {string} 상품이 존재한다")
    public void DB에_상품이_존재한다(String name) {
        var products = productRepository.findAll();
        assertThat(products).anyMatch(p -> p.getName().equals(name));
    }

    @그리고("데이터베이스에 상품이 없다")
    public void DB에_상품이_없다() {
        assertThat(productRepository.findAll()).isEmpty();
    }

    @그리고("응답 상품의 카테고리가 {string}이다")
    public void 응답_상품의_카테고리가(String categoryName) {
        context.getLastResponse().then()
                .body("category.name", equalTo(categoryName));
    }

    @그리고("모든 상품에 카테고리 {string}이 포함되어 있다")
    public void 모든_상품에_카테고리_포함(String categoryName) {
        context.getLastResponse().then()
                .body("category.name", everyItem(equalTo(categoryName)));
    }
}
