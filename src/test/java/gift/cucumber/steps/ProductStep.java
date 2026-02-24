package gift.cucumber.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStep {

    @Autowired
    private SharedContext context;

    @When("카테고리 {string}를 생성한 뒤 상품 {string}, 가격 {int}, 이미지 {string}을 생성한다")
    public void 카테고리_생성_뒤_상품_생성(String categoryName, String productName, int price, String imageUrl) {
        var categoryResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", categoryName))
                .when().post("/api/categories")
                .then().log().all().extract();
        long categoryId = categoryResponse.jsonPath().getLong("id");

        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", productName,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
                .when().post("/api/products")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @When("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        var response = RestAssured.given().log().all()
                .when().get("/api/products")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @And("응답 본문 상품 이름은 {string}이다")
    public void 응답_본문_상품_이름(String name) {
        assertThat(context.getResponse().jsonPath().getString("name")).isEqualTo(name);
    }

    @And("응답 본문 상품 가격은 {int}이다")
    public void 응답_본문_상품_가격(int price) {
        assertThat(context.getResponse().jsonPath().getInt("price")).isEqualTo(price);
    }

    @And("응답 본문 상품 카테고리 이름은 {string}이다")
    public void 응답_본문_상품_카테고리_이름(String categoryName) {
        assertThat(context.getResponse().jsonPath().getString("category.name")).isEqualTo(categoryName);
    }

    @And("응답 본문의 카테고리 이름 목록은 {string}, {string}이다")
    public void 응답_본문의_카테고리_이름_목록은(String name1, String name2) {
        List<String> names = context.getResponse().jsonPath().getList("category.name", String.class);
        assertThat(names).contains(name1, name2);
    }
}
