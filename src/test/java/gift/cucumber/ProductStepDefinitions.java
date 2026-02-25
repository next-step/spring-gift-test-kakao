package gift.cucumber;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class ProductStepDefinitions {

    @Autowired
    private SharedState sharedState;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @그리고("{string} 카테고리에 가격 {int}원인 {string} 상품이 등록되어 있다")
    public void 카테고리에_가격_원인_상품이_등록되어_있다(String categoryName, int price, String productName) {
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ?",
                Long.class,
                categoryName
        );
        jdbcTemplate.update(
                "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
                productName, price, "http://example.com/image.png", categoryId
        );
    }

    @만약("{string} 카테고리에 가격 {int}원인 {string} 상품을 생성하면")
    public void 카테고리에_가격_원인_상품을_생성하면(String categoryName, int price, String productName) {
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ?",
                Long.class,
                categoryName
        );
        sharedState.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", productName,
                        "price", price,
                        "imageUrl", "http://example.com/image.png",
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products"));
    }

    @만약("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        sharedState.setResponse(RestAssured.given()
                .when()
                .get("/api/products"));
    }

    @그리고("상품 목록에 {string}이 포함되어 있다")
    public void 상품_목록에_이_포함되어_있다(String productName) {
        sharedState.getResponse().then().body("name", hasItem(productName));
    }

    @그리고("상품 목록의 크기는 {int}이다")
    public void 상품_목록의_크기는_이다(int size) {
        sharedState.getResponse().then().body("", hasSize(size));
    }

    @그리고("{int}번째 상품의 가격은 {int}이다")
    public void n번째_상품의_가격은_이다(int index, int price) {
        sharedState.getResponse().then().body("[" + (index - 1) + "].price", equalTo(price));
    }

    @그리고("{int}번째 상품의 카테고리는 {string}이다")
    public void n번째_상품의_카테고리는_이다(int index, String categoryName) {
        sharedState.getResponse().then().body("[" + (index - 1) + "].category.name", equalTo(categoryName));
    }

    @만약("존재하지 않는 카테고리로 상품을 생성하면")
    public void 존재하지_않는_카테고리로_상품을_생성하면() {
        sharedState.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "떡볶이",
                        "price", 5000,
                        "imageUrl", "http://example.com/image.png",
                        "categoryId", 9999
                ))
                .when()
                .post("/api/products"));
    }
}
