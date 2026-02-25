package gift.cucumber;

import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.그러면;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class CategoryStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @먼저("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .post("/api/categories");
        long id = response.then().extract().jsonPath().getLong("id");
        sharedContext.putId("카테고리_" + name, id);
    }

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
        sharedContext.setLastResponse(response);
    }

    @만일("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        var response = RestAssured.given()
                .when()
                .get("/api/categories");
        sharedContext.setLastResponse(response);
    }

    @그러면("카테고리가 정상적으로 등록된다")
    public void 카테고리가_정상적으로_등록된다() {
        sharedContext.getLastResponse().then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @그러면("카테고리 목록에 {string}이 포함되어 있다")
    public void 카테고리_목록에_포함_이(String name) {
        카테고리_목록에_포함(name);
    }

    @그러면("카테고리 목록에 {string}가 포함되어 있다")
    public void 카테고리_목록에_포함_가(String name) {
        카테고리_목록에_포함(name);
    }

    private void 카테고리_목록에_포함(String name) {
        List<Map<String, Object>> items = RestAssured.given()
                .when()
                .get("/api/categories")
                .then().extract().jsonPath().getList("$");
        assertThat(items).anySatisfy(item ->
                assertThat(item.get("name")).isEqualTo(name));
    }
}
