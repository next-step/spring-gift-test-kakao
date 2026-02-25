package gift.cucumber;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    ScenarioContext scenarioContext;

    @만일("^\"([^\"]*)\" 상품을 가격 (\\d+), 이미지 \"([^\"]*)\", 카테고리 \"([^\"]*)\"으로 등록하면$")
    public void 상품을_등록하면(String name, int price, String imageUrl, String categoryName) {
        long categoryId = scenarioContext.getId(categoryName);
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": %d
                        }
                        """.formatted(name, price, imageUrl, categoryId))
                .when()
                .post("/api/products");

        scenarioContext.setResponse(response);
    }

    @만일("^\"([^\"]*)\" 상품을 가격 (\\d+), 이미지 \"([^\"]*)\", 존재하지 않는 카테고리로 등록하면$")
    public void 상품을_존재하지않는_카테고리로_등록하면(String name, int price, String imageUrl) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": 999
                        }
                        """.formatted(name, price, imageUrl))
                .when()
                .post("/api/products");

        scenarioContext.setResponse(response);
    }

    @그러면("^상품이 (\\d+)개 등록되어 있다$")
    public void 상품_개수를_확인한다(int expectedCount) {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        assertThat(response.jsonPath().getList("$")).hasSize(expectedCount);
    }

    @그러면("^등록된 상품 \"([^\"]*)\"의 카테고리는 \"([^\"]*)\"이다$")
    public void 상품의_카테고리를_확인한다(String productName, String expectedCategoryName) {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        String categoryName = response.jsonPath()
                .getList("findAll { it.name == '" + productName + "' }.category.name", String.class)
                .getFirst();

        assertThat(categoryName).isEqualTo(expectedCategoryName);
    }
}
