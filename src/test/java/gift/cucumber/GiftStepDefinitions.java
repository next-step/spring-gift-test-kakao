package gift.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class GiftStepDefinitions {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ScenarioContext scenarioContext;

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

}
