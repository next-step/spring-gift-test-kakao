package gift.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ScenarioContext scenarioContext;

    @Before
    public void setUp() {
        RestAssured.port = port;
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
    }

    @Given("^회원 \"([^\"]*)\"\\(ID: (\\d+)\\)과 \"([^\"]*)\"\\(ID: (\\d+)\\)이 존재한다$")
    public void 회원이_존재한다(String name1, long id1, String name2, long id2) {
        jdbcTemplate.update("INSERT INTO member (id, name, email) VALUES (?, ?, ?)",
                id1, name1, "member" + id1 + "@test.com");
        jdbcTemplate.update("INSERT INTO member (id, name, email) VALUES (?, ?, ?)",
                id2, name2, "member" + id2 + "@test.com");
    }

    @Given("^카테고리 \"([^\"]*)\"\\(ID: (\\d+)\\)이 존재한다$")
    public void 카테고리가_존재한다(String name, long id) {
        jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", id, name);
    }

    @Given("^상품 \"([^\"]*)\"\\(가격: (\\d+), 카테고리ID: (\\d+)\\)이 존재한다$")
    public void 상품이_존재한다(String name, int price, long categoryId) {
        jdbcTemplate.update(
                "INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, ?, ?, 'img.jpg', ?)",
                name, price, categoryId);
    }

    @Then("^응답 상태 코드는 (\\d+)이다$")
    public void 응답_상태_코드를_확인한다(int expectedStatusCode) {
        assertThat(scenarioContext.getResponseStatusCode()).isEqualTo(expectedStatusCode);
    }
}
