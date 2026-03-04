package gift.steps;

import gift.model.Member;
import gift.model.Option;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

public class GiftStepDefs {

    @Autowired
    private ScenarioState state;

    @When("{string}이 {string}에게 {string} 옵션을 {int}개 선물한다")
    public void giveGift(final String senderName, final String receiverName, final String optionName, final int quantity) {
        final Member sender = state.get("member:" + senderName);
        final Member receiver = state.get("member:" + receiverName);
        final Option option = state.get("option:" + optionName);

        final String body = String.format("""
                { "optionId": %d, "quantity": %d, "receiverId": %d, "message": "선물입니다!" }
                """, option.getId(), quantity, receiver.getId());

        state.setResponse(
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", sender.getId())
                        .body(body)
                .when()
                        .post("/api/gifts")
        );
    }
}
