package gift.cucumber.steps;

import gift.cucumber.TestContext;
import gift.model.Category;
import gift.model.CategoryRepository;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.cucumber.java.ko.그리고;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.hamcrest.Matchers.hasItems;

public class CategorySteps {

    @Value("${cucumber.target.url}")
    private String targetUrl;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestContext testContext;

    @조건("{string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(String name) {
        Category category = categoryRepository.save(new Category(name));
        testContext.setLastCategory(category);
    }

    @만일("{string} 이름으로 카테고리를 생성하면")
    public void 이름으로_카테고리를_생성하면(String name) {
        String body = String.format("""
                {"name": "%s"}
                """, name);
        Response response = RestAssured.given()
                .baseUri(targetUrl)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/categories");
        testContext.setLastResponse(response);
    }

    @만일("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        Response response = RestAssured.given()
                .baseUri(targetUrl)
                .when()
                .get("/api/categories");
        testContext.setLastResponse(response);
    }

    @그리고("목록에 {string} 이름이 포함되어 있다")
    public void 목록에_이름이_포함되어_있다(String name) {
        testContext.getLastResponse().then().body("name", hasItems(name));
    }
}
