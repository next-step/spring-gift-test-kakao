package gift.acceptance.steps;

import gift.acceptance.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class ProductSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private TestContext testContext;

    private Response response;

    @먼저("카테고리에 이름이 {string}이고 가격이 {int}인 상품을 생성한다")
    public void 상품을_생성한다(String name, int price) {
        Long productId = RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "%s", "price": %d, "imageUrl": "https://example.com/image.jpg", "categoryId": %d}
                        """, name, price, testContext.getCategoryId()))
                .post("/api/products")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setProductId(productId);
    }

    @만일("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        response = RestAssured.given()
                .port(port)
                .when()
                .get("/api/products");
    }

    @그러면("상품 목록에 {string}이 포함되어 있다")
    public void 상품_목록에_이름이_포함되어_있다(String name) {
        response.then()
                .statusCode(200)
                .body("name", hasItem(name));
    }

    @그리고("상품 목록의 크기는 {int}이다")
    public void 상품_목록의_크기는(int size) {
        response.then()
                .body("", hasSize(size));
    }
}
