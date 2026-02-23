package gift.cucumber;

import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.cucumber.java.ko.그러면;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ProductStepDefinitions {

    private final ScenarioContext scenarioContext;
    private final JdbcTemplate jdbcTemplate;

    public ProductStepDefinitions(ScenarioContext scenarioContext, JdbcTemplate jdbcTemplate) {
        this.scenarioContext = scenarioContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @조건("{string} 카테고리에 {string} 상품이 등록되어 있다")
    public void 상품이_등록되어_있다(String categoryName, String productName) {
        long categoryId = scenarioContext.getCategoryId(categoryName);
        long id = scenarioContext.nextProductId();
        jdbcTemplate.update(
                "INSERT INTO product (id, name, price, image_url, category_id) VALUES (?, ?, ?, ?, ?)",
                id, productName, 4500, "https://example.com/img.jpg", categoryId
        );
        scenarioContext.putProductId(productName, id);
    }

    @만일("{string} 상품을 가격 {int}원, 이미지 {string}, 카테고리 {string}로 생성하면")
    public void 상품을_생성하면(String name, int price, String imageUrl, String categoryName) {
        long categoryId = scenarioContext.getCategoryId(categoryName);
        var response = given()
                .queryParam("name", name)
                .queryParam("price", price)
                .queryParam("imageUrl", imageUrl)
                .queryParam("categoryId", categoryId)
                .when()
                .post("/api/products")
                .then()
                .extract();
        scenarioContext.setResponse(response);
    }

    @만일("존재하지 않는 카테고리로 상품을 생성하면")
    public void 존재하지_않는_카테고리로_상품을_생성하면() {
        var response = given()
                .queryParam("name", "아메리카노")
                .queryParam("price", 4500)
                .queryParam("imageUrl", "https://example.com/img.jpg")
                .queryParam("categoryId", 999)
                .when()
                .post("/api/products")
                .then()
                .extract();
        scenarioContext.setResponse(response);
    }

    @그러면("상품 목록을 조회하면 {int}개가 존재한다")
    public void 상품_목록을_조회하면(int count) {
        var response = given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body(".", hasSize(count))
                .extract();
        scenarioContext.setResponse(response);
    }

    @그러면("상품 목록에 {string}가 포함되어 있다")
    public void 상품_목록에_포함(String name) {
        var names = scenarioContext.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }
}
