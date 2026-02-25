package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.CategoryRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class CategoryStepDefinitions {

    @Autowired
    ScenarioContext context;

    @Autowired
    CategoryRepository categoryRepository;

    @만일("{string} 카테고리를 등록한다")
    public void 카테고리를_등록한다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .post("/api/categories");

        context.setLastResponse(response);
    }

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/categories");

        context.setLastResponse(response);
    }

    @그러면("등록이 성공한다")
    public void 등록이_성공한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(200);
    }

    @그러면("등록이 실패한다")
    public void 등록이_실패한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(500);
    }

    @그러면("조회가 성공한다")
    public void 조회가_성공한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(200);
    }

    @그리고("응답에 카테고리 이름 {string}이 포함되어 있다")
    public void 응답에_카테고리_이름이_포함(String name) {
        context.getLastResponse().then()
                .body("name", equalTo(name));
    }

    @그리고("데이터베이스에 {string} 카테고리가 존재한다")
    public void DB에_카테고리가_존재한다(String name) {
        var categories = categoryRepository.findAll();
        assertThat(categories).anyMatch(c -> c.getName().equals(name));
    }

    @그리고("응답 목록이 비어있다")
    public void 응답_목록이_비어있다() {
        context.getLastResponse().then()
                .body("$", hasSize(0));
    }

    @그리고("응답 목록에 {string}, {string}가 포함되어 있다")
    public void 응답_목록에_포함(String name1, String name2) {
        context.getLastResponse().then()
                .body("name", hasItems(name1, name2));
    }
}
