package steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.*;

public class CommonSteps {

    private final ScenarioContext context;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    @Value("${local.server.port:0}")
    private int port;

    public CommonSteps(ScenarioContext context, JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        if (appBaseUrl != null && !appBaseUrl.isBlank()) {
            RestAssured.baseURI = appBaseUrl;
        } else {
            RestAssured.baseURI = "http://localhost";
            RestAssured.port = port;
        }
        jdbcTemplate.execute("DELETE FROM wish");
        jdbcTemplate.execute("DELETE FROM options");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM member");
    }

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드_검증(int statusCode) {
        context.getResponse().then()
            .statusCode(statusCode);
    }

    @Then("응답의 {string} 필드는 null이 아니다")
    public void 응답_필드_not_null(String field) {
        context.getResponse().then()
            .body(field, notNullValue());
    }

    @Then("응답의 {string} 필드는 {string}이다")
    public void 응답_필드_문자열_검증(String field, String expected) {
        context.getResponse().then()
            .body(field, equalTo(expected));
    }

    @Then("응답의 정수 필드 {string}는 {int}이다")
    public void 응답_정수_필드_검증(String field, int expected) {
        context.getResponse().then()
            .body(field, equalTo(expected));
    }

    @Then("응답 목록의 크기는 {int}이다")
    public void 응답_목록_크기_검증(int size) {
        context.getResponse().then()
            .body("size()", equalTo(size));
    }

    @Then("응답 목록의 {string}에 {string}, {string}이 순서 무관하게 포함되어 있다")
    public void 응답_목록_순서_무관_포함(String field, String value1, String value2) {
        context.getResponse().then()
            .body(field, containsInAnyOrder(value1, value2));
    }

    @Then("응답 목록의 정수 {string}에 {int}, {int}이 순서 무관하게 포함되어 있다")
    public void 응답_목록_정수_순서_무관_포함(String field, int value1, int value2) {
        context.getResponse().then()
            .body(field, containsInAnyOrder(value1, value2));
    }

    @Then("응답 목록의 모든 {string}는 null이 아니다")
    public void 응답_목록_전체_not_null(String field) {
        context.getResponse().then()
            .body(field, everyItem(notNullValue()));
    }

    @Then("응답 목록의 모든 {string}은 {string}이다")
    public void 응답_목록_전체_값_일치(String field, String expected) {
        context.getResponse().then()
            .body(field, everyItem(equalTo(expected)));
    }
}
