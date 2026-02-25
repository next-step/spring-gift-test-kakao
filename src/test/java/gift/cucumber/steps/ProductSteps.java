package gift.cucumber.steps;

import gift.cucumber.TestContext;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class ProductSteps {

    @Value("${cucumber.target.url}")
    private String targetUrl;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestContext testContext;

    @그리고("{string} 카테고리에 상품이 등록되어 있다")
    public void 카테고리에_상품이_등록되어_있다(String categoryName, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        Product product = null;
        for (Map<String, String> row : rows) {
            product = productRepository.save(new Product(
                    row.get("name"),
                    Integer.parseInt(row.get("price")),
                    row.get("imageUrl"),
                    testContext.getLastCategory()
            ));
        }
        testContext.setLastProduct(product);
    }

    @만일("상품 목록을 조회하면")
    public void 상품_목록을_조회하면() {
        Response response = RestAssured.given()
                .baseUri(targetUrl)
                .when()
                .get("/api/products");
        testContext.setLastResponse(response);
    }

    @그리고("첫 번째 상품의 이름은 {string}이다")
    public void 첫_번째_상품의_이름은_이다(String name) {
        testContext.getLastResponse().then().body("[0].name", equalTo(name));
    }

    @그리고("첫 번째 상품의 가격은 {int}이다")
    public void 첫_번째_상품의_가격은_이다(int price) {
        testContext.getLastResponse().then().body("[0].price", equalTo(price));
    }

    @그리고("첫 번째 상품의 카테고리는 {string}이다")
    public void 첫_번째_상품의_카테고리는_이다(String categoryName) {
        testContext.getLastResponse().then().body("[0].category.name", equalTo(categoryName));
    }
}
