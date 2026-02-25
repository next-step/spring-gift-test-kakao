package gift.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;

public class GiftStepDefinitions {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ScenarioContext scenarioContext;

    @Given("^상품 \"([^\"]*)\"에 옵션 \"([^\"]*)\"의 재고가 (\\d+)개이다$")
    public void 옵션의_재고가_존재한다(String productName, String optionName, int quantity) {
        long productId = scenarioContext.getId(productName);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO option (name, quantity, product_id) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, optionName);
            ps.setInt(2, quantity);
            ps.setLong(3, productId);
            return ps;
        }, keyHolder);
        scenarioContext.storeId(optionName, keyHolder.getKey().longValue());
    }

    @When("^\"([^\"]*)\"이 옵션 \"([^\"]*)\"을 (\\d+)개 \"([^\"]*)\"에게 \"([^\"]*)\" 메시지와 함께 선물하면$")
    public void 선물을_보낸다(String senderName, String optionName, int quantity, String receiverName, String message) {
        long senderId = scenarioContext.getId(senderName);
        long optionId = scenarioContext.getId(optionName);
        long receiverId = scenarioContext.getId(receiverName);

        Response response = RestAssured.given()
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
                .post("/api/gifts");

        scenarioContext.setResponse(response);
    }
}
