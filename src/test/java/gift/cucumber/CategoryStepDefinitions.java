package gift.cucumber;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @먼저("{string} 카테고리가 존재한다")
    public void 카테고리가_존재한다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then()
                .extract();
        assertThat(response.statusCode()).isEqualTo(200);
        context.putCategoryId(name, response.jsonPath().getLong("id"));
    }

    @만일("{string} 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then()
                .extract();
        context.setResponse(response);
    }

    @만일("카테고리 목록을 조회하면")
    public void 카테고리_목록을_조회하면() {
        var response = RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .extract();
        context.setResponse(response);
    }

    @그러면("카테고리가 생성된다")
    public void 카테고리가_생성된다() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
        assertThat(context.getResponse().jsonPath().getLong("id")).isNotNull();
    }

    @그러면("카테고리 이름은 {string}이다")
    public void 카테고리_이름은_이다(String name) {
        assertThat(context.getResponse().jsonPath().getString("name")).isEqualTo(name);
    }

    @그러면("{string}, {string} 카테고리가 조회된다")
    public void 카테고리가_조회된다(String name1, String name2) {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("id")).doesNotContainNull();
        assertThat(response.jsonPath().getList("name"))
                .containsExactlyInAnyOrder(name1, name2);
    }

    @그러면("카테고리가 조회되지 않는다")
    public void 카테고리가_조회되지_않는다() {
        var response = context.getResponse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("$")).isEmpty();
    }
}
