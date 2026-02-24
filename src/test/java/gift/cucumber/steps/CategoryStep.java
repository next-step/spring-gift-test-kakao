package gift.cucumber.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStep {

    @Autowired
    private SharedContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Given("빈 데이터 상태다")
    public void 빈_데이터_상태다() {
        // DatabaseCleanUp @Before 훅에서 이미 처리됨
    }

    @Given("기본 테스트 데이터가 준비되어 있다")
    public void 기본_테스트_데이터가_준비되어_있다() {
        jdbcTemplate.execute("INSERT INTO category (id, name) VALUES (1, '간식')");
        jdbcTemplate.execute("INSERT INTO category (id, name) VALUES (2, '음료')");
        jdbcTemplate.execute("INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 5000, 'http://img.com/choco.png', 1)");
        jdbcTemplate.execute("INSERT INTO product (id, name, price, image_url, category_id) VALUES (2, '커피', 3000, 'http://img.com/coffee.png', 2)");
        jdbcTemplate.execute("INSERT INTO option (id, name, quantity, product_id) VALUES (1, '초콜릿 기본', 10, 1)");
        jdbcTemplate.execute("INSERT INTO option (id, name, quantity, product_id) VALUES (2, '커피 기본', 1, 2)");
        jdbcTemplate.execute("INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com')");
        jdbcTemplate.execute("INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com')");
    }

    @When("카테고리 {string}를 생성한다")
    public void 카테고리를_생성한다(String name) {
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when().post("/api/categories")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @When("카테고리 목록을 조회한다")
    public void 카테고리_목록을_조회한다() {
        var response = RestAssured.given().log().all()
                .when().get("/api/categories")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @Then("응답 코드는 {int}이다")
    public void 응답_코드는(int statusCode) {
        assertThat(context.getResponse().statusCode()).isEqualTo(statusCode);
    }

    @And("응답 본문의 이름 목록에는 {string}가 포함된다")
    public void 응답_본문의_이름_목록에는_포함된다(String name) {
        List<String> names = context.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name);
    }

    @And("응답 본문의 이름 목록은 {string}, {string}이다")
    public void 응답_본문의_이름_목록은(String name1, String name2) {
        List<String> names = context.getResponse().jsonPath().getList("name", String.class);
        assertThat(names).contains(name1, name2);
    }
}
