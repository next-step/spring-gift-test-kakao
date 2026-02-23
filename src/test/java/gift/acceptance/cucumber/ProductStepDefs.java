package gift.acceptance.cucumber;

import io.cucumber.java.ko.만일;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class ProductStepDefs {

    @Autowired
    SharedContext context;

    @만일("카테고리 {int}번으로 {string} 상품을 생성하면")
    public void 카테고리_번으로_상품을_생성하면(int categoryId, String name) {
        var response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", 5000,
                "imageUrl", "http://test.com/new.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products");
        context.setResponse(response);
    }

    @만일("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        var response = given()
        .when()
            .get("/api/products");
        context.setResponse(response);
    }
}
