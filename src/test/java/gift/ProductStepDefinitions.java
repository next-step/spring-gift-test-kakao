package gift;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

public class ProductStepDefinitions {

    @그리고("{string} 카테고리에 {string} 상품이 존재한다")
    public void 상품이_존재한다(String categoryName, String productName) throws Exception {
        Long categoryId = SharedContext.getCategoryId(categoryName);
        try (Connection conn = DriverManager.getConnection(
                SharedContext.DB_URL, SharedContext.DB_USER, SharedContext.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO product (name, price, image_url, category_id) VALUES (?, 4500, 'http://example.com/image.png', ?)")) {
            stmt.setString(1, productName);
            stmt.setLong(2, categoryId);
            stmt.executeUpdate();
        }
    }

    @만일("{string} 카테고리에 {int}원짜리 {string} 상품을 생성한다")
    public void 상품을_생성한다(String categoryName, int price, String productName) {
        Long categoryId = SharedContext.getCategoryId(categoryName);
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .contentType("application/json")
                .body(Map.of(
                        "name", productName,
                        "price", price,
                        "imageUrl", "http://example.com/image.png",
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }

    @만일("존재하지 않는 카테고리로 상품을 생성한다")
    public void 존재하지_않는_카테고리로_상품을_생성한다() {
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .contentType("application/json")
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "http://example.com/image.png",
                        "categoryId", 999
                ))
                .when()
                .post("/api/products")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }

    @만일("상품을 전체 조회한다")
    public void 상품을_전체_조회한다() {
        var response = RestAssured.given().log().all()
                .baseUri(SharedContext.BASE_URL)
                .when()
                .get("/api/products")
                .then().log().all()
                .extract();
        SharedContext.setResponse(response);
    }
}
