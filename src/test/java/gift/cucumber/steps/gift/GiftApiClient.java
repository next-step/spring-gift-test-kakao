package gift.cucumber.steps.gift;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class GiftApiClient {

    public Response sendGift(final Long senderId, final Long optionId, final int quantity, final Long receiverId, final String message) {
        return given()
            .contentType(ContentType.JSON)
            .header("Member-Id", senderId)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", receiverId,
                "message", message
            ))
            .when()
            .post("/api/gifts");
    }

    public Response sendGiftWithoutHeader() {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "optionId", 1L,
                "quantity", 1,
                "receiverId", 1L,
                "message", "선물"
            ))
            .when()
            .post("/api/gifts");
    }
}
