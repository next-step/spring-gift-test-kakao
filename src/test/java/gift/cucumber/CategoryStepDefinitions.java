package gift.cucumber;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @만약("이름이 {string}인 카테고리를 생성한다")
    public void 이름이_인_카테고리를_생성한다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories");
        sharedContext.setResponse(response);
    }

    @그리고("응답에 이름이 {string}인 카테고리가 포함되어 있다")
    public void 응답에_이름이_인_카테고리가_포함되어_있다(String name) {
        var response = sharedContext.getResponse();
        assertThat(response.jsonPath().getString("name")).isEqualTo(name);
        assertThat(response.jsonPath().getLong("id")).isNotNull();
    }

    @만약("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/categories");
        sharedContext.setResponse(response);
    }

    @그리고("카테고리 목록에 {string}가 포함되어 있다")
    public void 카테고리_목록에_가_포함되어_있다(String name) {
        var response = sharedContext.getResponse();
        var names = response.jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }

    @조건("카테고리 {string}가 존재한다")
    public void 카테고리가_존재한다(String name) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200);
    }
}
