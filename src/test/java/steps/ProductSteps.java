package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class ProductSteps {

    private final ScenarioContext context;

    public ProductSteps(ScenarioContext context) {
        this.context = context;
    }

    private Long getCategoryId(String categoryName) {
        return given()
        .when()
            .get("/api/categories")
        .then()
            .extract()
            .jsonPath()
            .getList("findAll { it.name == '" + categoryName + "' }.id", Long.class)
            .get(0);
    }

    @Given("카테고리 {string}에 이름 {string} 가격 {int} 이미지 {string}인 상품이 등록되어 있다")
    public void 상품_등록(String categoryName, String name, int price, String imageUrl) {
        var categoryId = getCategoryId(categoryName);
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);
    }

    @When("카테고리 {string}에 이름 {string} 가격 {int} 이미지 {string}인 상품을 생성하면")
    public void 상품_생성(String categoryName, String name, int price, String imageUrl) {
        var categoryId = getCategoryId(categoryName);
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "name", name,
                    "price", price,
                    "imageUrl", imageUrl,
                    "categoryId", categoryId
                ))
            .when()
                .post("/api/products")
        );
    }

    @When("존재하지 않는 카테고리로 상품을 생성하면")
    public void 존재하지_않는_카테고리로_상품_생성() {
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "name", "노트북",
                    "price", 1_500_000,
                    "imageUrl", "https://example.com/notebook.png",
                    "categoryId", 9999L
                ))
            .when()
                .post("/api/products")
        );
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록_조회() {
        context.setResponse(
            given()
            .when()
                .get("/api/products")
        );
    }
}
