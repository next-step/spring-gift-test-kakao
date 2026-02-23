package gift.cucumber;

import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.cucumber.java.ko.그러면;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class CategoryStepDefinitions {

    private final ScenarioContext scenarioContext;
    private final JdbcTemplate jdbcTemplate;

    public CategoryStepDefinitions(ScenarioContext scenarioContext, JdbcTemplate jdbcTemplate) {
        this.scenarioContext = scenarioContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @조건("{string} 카테고리가 등록되어 있다")
    public void 카테고리가_등록되어_있다(String name) {
        long id = scenarioContext.nextCategoryId();
        jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", id, name);
        scenarioContext.putCategoryId(name, id);
    }

    @만일("{string} 카테고리를 생성하면")
    public void 카테고리를_생성하면(String name) {
        var response = given()
                .queryParam("name", name)
                .when()
                .post("/api/categories")
                .then()
                .extract();
        scenarioContext.setResponse(response);
    }

    @그러면("카테고리 목록을 조회하면 {int}개가 존재한다")
    public void 카테고리_목록을_조회하면(int count) {
        var response = given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body(".", hasSize(count))
                .extract();
        scenarioContext.setResponse(response);
    }

    @그러면("카테고리 목록에 {string}가 포함되어 있다")
    public void 카테고리_목록에_포함(String name) {
        var names = scenarioContext.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }
}
