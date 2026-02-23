package gift.cucumber;

import gift.model.Option;
import gift.model.OptionRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ScenarioContext scenarioContext;

    @Before
    public void setUp() {
        RestAssured.port = port;
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE option");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
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

    @Given("^옵션 \"([^\"]*)\"의 재고가 (\\d+)개이다$")
    public void 옵션의_재고가_존재한다(String name, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO option (id, name, quantity, product_id) VALUES (1, ?, ?, 1)",
                name, quantity);
    }

    @When("^회원 (\\d+)이 옵션 (\\d+)을 (\\d+)개 회원 (\\d+)에게 \"([^\"]*)\" 메시지와 함께 선물하면$")
    public void 선물을_보낸다(long senderId, long optionId, int quantity, long receiverId, String message) {
        int statusCode = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "%s"
                        }
                        """.formatted(optionId, quantity, receiverId, message))
                .when()
                .post("/api/gifts")
                .then()
                .extract()
                .statusCode();

        scenarioContext.setResponseStatusCode(statusCode);
    }

    @Then("^응답 상태 코드는 (\\d+)이다$")
    public void 응답_상태_코드를_확인한다(int expectedStatusCode) {
        assertThat(scenarioContext.getResponseStatusCode()).isEqualTo(expectedStatusCode);
    }

    @Then("^옵션 (\\d+)의 재고는 (\\d+)개이다$")
    public void 옵션의_재고를_확인한다(long optionId, int expectedQuantity) {
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
