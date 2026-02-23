package gift.cucumber;

import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;

public class GiftStepDefinitions {

    private final ScenarioContext scenarioContext;
    private final JdbcTemplate jdbcTemplate;

    public GiftStepDefinitions(ScenarioContext scenarioContext, JdbcTemplate jdbcTemplate) {
        this.scenarioContext = scenarioContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @조건("{string} 상품에 {string} 옵션이 재고 {int}개로 등록되어 있다")
    public void 옵션이_등록되어_있다(String productName, String optionName, int quantity) {
        long productId = scenarioContext.getProductId(productName);
        long id = scenarioContext.nextOptionId();
        jdbcTemplate.update(
                "INSERT INTO option (id, name, quantity, product_id) VALUES (?, ?, ?, ?)",
                id, optionName, quantity, productId
        );
        scenarioContext.putOptionId(optionName, id);
    }

    @조건("{string}과 {string} 회원이 등록되어 있다")
    public void 회원이_등록되어_있다(String sender, String receiver) {
        long senderId = scenarioContext.nextMemberId();
        jdbcTemplate.update(
                "INSERT INTO member (id, name, email) VALUES (?, ?, ?)",
                senderId, sender, sender + "@test.com"
        );
        scenarioContext.putMemberId(sender, senderId);

        long receiverId = scenarioContext.nextMemberId();
        jdbcTemplate.update(
                "INSERT INTO member (id, name, email) VALUES (?, ?, ?)",
                receiverId, receiver, receiver + "@test.com"
        );
        scenarioContext.putMemberId(receiver, receiverId);
    }

    @만일("{string}이 {string}에게 {string} 옵션으로 {int}개를 선물하면")
    public void 선물하면(String sender, String receiver, String optionName, int quantity) {
        long senderId = scenarioContext.getMemberId(sender);
        long receiverId = scenarioContext.getMemberId(receiver);
        long optionId = scenarioContext.getOptionId(optionName);

        String body = """
                {
                    "optionId": %d,
                    "quantity": %d,
                    "receiverId": %d,
                    "message": "테스트 선물"
                }
                """.formatted(optionId, quantity, receiverId);

        var response = given()
                .header("Member-Id", senderId)
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/gifts")
                .then()
                .extract();
        scenarioContext.setResponse(response);
    }

    @만일("{string}이 {string}에게 존재하지 않는 옵션으로 {int}개를 선물하면")
    public void 존재하지_않는_옵션으로_선물하면(String sender, String receiver, int quantity) {
        long senderId = scenarioContext.getMemberId(sender);
        long receiverId = scenarioContext.getMemberId(receiver);

        String body = """
                {
                    "optionId": 999,
                    "quantity": %d,
                    "receiverId": %d,
                    "message": "테스트 선물"
                }
                """.formatted(quantity, receiverId);

        var response = given()
                .header("Member-Id", senderId)
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/gifts")
                .then()
                .extract();
        scenarioContext.setResponse(response);
    }
}
