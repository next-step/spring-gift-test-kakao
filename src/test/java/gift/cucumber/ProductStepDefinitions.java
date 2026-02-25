package gift.cucumber;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @만약("{string} 카테고리에 이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품을 생성한다")
    public void 카테고리에_상품을_생성한다(String categoryName, String name, int price, String imageUrl) {
        var categoriesResponse = RestAssured.given()
                .when()
                .get("/api/categories");
        long categoryId = categoriesResponse.jsonPath().getLong("find { it.name == '" + categoryName + "' }.id");

        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
                .when()
                .post("/api/products");
        sharedContext.setResponse(response);
    }

    @만약("존재하지 않는 카테고리에 이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품을 생성한다")
    public void 존재하지_않는_카테고리에_상품을_생성한다(String name, int price, String imageUrl) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", 9999L
                ))
                .when()
                .post("/api/products");
        sharedContext.setResponse(response);
    }

    @그리고("응답에 이름이 {string}이고 가격이 {int}인 상품이 포함되어 있다")
    public void 응답에_상품이_포함되어_있다(String name, int price) {
        var response = sharedContext.getResponse();
        assertThat(response.jsonPath().getString("name")).isEqualTo(name);
        assertThat(response.jsonPath().getInt("price")).isEqualTo(price);
    }

    @만약("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/products");
        sharedContext.setResponse(response);
    }

    @그리고("상품 목록에 {string}가 포함되어 있다")
    public void 상품_목록에_포함되어_있다(String name) {
        var response = sharedContext.getResponse();
        var names = response.jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }

    @그리고("상품 목록이 비어있다")
    public void 상품_목록이_비어있다() {
        var response = sharedContext.getResponse();
        var list = response.jsonPath().getList("$");
        assertThat(list).isEmpty();
    }
}
