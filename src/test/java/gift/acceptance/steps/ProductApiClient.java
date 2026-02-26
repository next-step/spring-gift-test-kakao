package gift.acceptance.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class ProductApiClient {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Response 상품을_등록한다(String name, int price, String imageUrl, Long categoryId) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");
    }

    public Response 상품을_이름없이_등록한다(int price, String imageUrl, Long categoryId) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");
    }

    public Response 상품_목록을_조회한다() {
        return given()
        .when()
            .get("/api/products");
    }

    /** TODO: POST /api/products 응답에서 ID 추출로 교체 가능 */
    public Long 상품을_DB에_등록한다(String name, int price, String imageUrl, Long categoryId) {
        jdbcTemplate.update(
            "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
            name, price, imageUrl, categoryId);
        return jdbcTemplate.queryForObject(
            "SELECT id FROM product WHERE name = ?", Long.class, name);
    }
}
