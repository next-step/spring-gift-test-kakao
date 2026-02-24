package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @만일("존재하지 않는 카테고리로 상품을 생성한다")
    public void 존재하지_않는_카테고리로_상품을_생성한다() {
        Response response = RestAssured.given()
                .formParam("name", "테스트상품")
                .formParam("price", 10000)
                .formParam("imageUrl", "http://image.url")
                .formParam("categoryId", 9999)
                .when()
                .post("/api/products");

        context.set("lastResponse", response);
    }

    @그러면("상품 생성이 실패한다")
    public void 상품_생성이_실패한다() {
        Response response = context.get("lastResponse", Response.class);
        assertThat(response.statusCode()).isEqualTo(500);
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        context.set("lastResponse", response);
    }

    @그러면("상품 목록이 비어있다")
    public void 상품_목록이_비어있다() {
        Response response = context.get("lastResponse", Response.class);
        response.then()
                .statusCode(200)
                .body("$", empty());
    }
}
