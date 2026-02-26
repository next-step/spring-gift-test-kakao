package gift.acceptance.product;

import gift.acceptance.AcceptanceContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class ProductStepDefinitions {

    @Autowired
    private AcceptanceContext context;

    @그리고("이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품이 등록되어 있다")
    public void 이름이_이고_가격이_이고_이미지가_인_상품이_등록되어_있다(String name, int price, String imageUrl) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", context.getCategoryId()
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(200);
    }

    @만약("이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품 생성을 요청하면")
    public void 이름이_이고_가격이_이고_이미지가_인_상품_생성을_요청하면(String name, int price, String imageUrl) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", context.getCategoryId()
                ))
        .when()
                .post("/api/products")
        .then()
                .extract();

        context.setResponse(response);
    }

    @만약("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        var response = given()
        .when()
                .get("/api/products")
        .then()
                .extract();

        context.setResponse(response);
    }

    @만약("존재하지 않는 카테고리로 상품 생성을 요청하면")
    public void 존재하지_않는_카테고리로_상품_생성을_요청하면() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아메리카노",
                        "price", 4500,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", 999999
                ))
        .when()
                .post("/api/products")
        .then()
                .extract();

        context.setResponse(response);
    }
}