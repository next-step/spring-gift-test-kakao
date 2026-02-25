package gift.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ScenarioContext scenarioContext;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE");
    }

    @Given("^회원 \"([^\"]*)\"과 \"([^\"]*)\"이 존재한다$")
    public void 회원이_존재한다(String name1, String name2) {
        long id1 = insertAndReturnId(
                "INSERT INTO member (name, email) VALUES (?, ?)",
                name1, name1 + "@test.com");
        scenarioContext.storeId(name1, id1);

        long id2 = insertAndReturnId(
                "INSERT INTO member (name, email) VALUES (?, ?)",
                name2, name2 + "@test.com");
        scenarioContext.storeId(name2, id2);
    }

    @Given("^카테고리 \"([^\"]*)\"이 존재한다$")
    public void 카테고리가_존재한다(String name) {
        long id = insertAndReturnId(
                "INSERT INTO category (name) VALUES (?)", name);
        scenarioContext.storeId(name, id);
    }

    @Given("^상품 \"([^\"]*)\"\\(가격: (\\d+), 카테고리: \"([^\"]*)\"\\)이 존재한다$")
    public void 상품이_존재한다(String name, int price, String categoryName) {
        long categoryId = scenarioContext.getId(categoryName);
        long id = insertAndReturnId(
                "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, 'img.jpg', ?)",
                name, price, categoryId);
        scenarioContext.storeId(name, id);
    }

    @Then("^응답 상태 코드는 (\\d+)이다$")
    public void 응답_상태_코드를_확인한다(int expectedStatusCode) {
        assertThat(scenarioContext.getResponse().statusCode()).isEqualTo(expectedStatusCode);
    }

    private long insertAndReturnId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
