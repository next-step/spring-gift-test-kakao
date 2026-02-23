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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class CategorySteps {

    @Autowired
    private TestContext testContext;

    private Response response;

    @먼저("{string} 카테고리를 생성한다")
    public void 카테고리를_생성한다(String name) {
        Long categoryId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "%s"}
                        """, name))
                .post("/api/categories")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setCategoryId(categoryId);
    }

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        response = RestAssured.given()
                .when()
                .get("/api/categories");
    }

    @그러면("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_이름이_포함되어_있다(String name) {
        response.then()
                .statusCode(200)
                .body("name", hasItem(name));
    }

    @그리고("카테고리 목록의 크기는 {int}이다")
    public void 카테고리_목록의_크기는(int size) {
        response.then()
                .body("", hasSize(size));
    }
}
