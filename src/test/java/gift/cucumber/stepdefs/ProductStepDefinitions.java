package gift.cucumber.stepdefs;

import gift.cucumber.ScenarioContext;
import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @When("이름이 {string}이고 가격이 {int}이고 카테고리가 {string}인 상품을 등록한다")
    public void 상품을_등록한다(String name, int price, String categoryName) {
        Category category = categoryRepository.findAll().stream()
            .filter(c -> c.getName().equals(categoryName))
            .findFirst()
            .orElseThrow();

        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "%s", "price": %d, "imageUrl": "img.jpg", "categoryId": %d}
                    """.formatted(name, price, category.getId()))
            .when()
                .post("/api/products")
        );
    }

    @When("이름이 {string}이고 가격이 {int}이고 존재하지 않는 카테고리로 상품을 등록한다")
    public void 존재하지_않는_카테고리로_상품을_등록한다(String name, int price) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "%s", "price": %d, "imageUrl": "img.jpg", "categoryId": 999}
                    """.formatted(name, price))
            .when()
                .post("/api/products")
        );
    }

    @When("가격에 {string}을 담아 상품 등록 요청을 보낸다")
    public void 가격에_잘못된값을_담아_상품_등록_요청을_보낸다(String invalidPrice) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "초콜릿", "price": "%s", "imageUrl": "img.jpg", "categoryId": 1}
                    """.formatted(invalidPrice))
            .when()
                .post("/api/products")
        );
    }

    @And("상품이 {int}개 저장되어 있다")
    public void 상품이_N개_저장되어_있다(int expectedCount) {
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(expectedCount);
    }

    @And("상품 {string}의 카테고리가 {string}이다")
    public void 상품의_카테고리가_N이다(String productName, String categoryName) {
        List<Product> products = productRepository.findAll();
        assertThat(products)
            .anyMatch(p -> p.getName().equals(productName) && p.getCategory().getName().equals(categoryName));
    }
}
