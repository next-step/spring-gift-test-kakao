package gift.cucumber.steps;

import gift.cucumber.TestContext;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class GiftSteps {

    @Autowired
    private TestContext context;

    @When("수량 {int}개로 선물을 보내면")
    public void 선물을_보내면(int quantity) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", quantity,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("존재하지 않는 옵션 ID로 선물을 보내면")
    public void 존재하지_않는_옵션_ID로_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", 999999L,
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("옵션 ID 없이 선물을 보내면")
    public void 옵션_ID_없이_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("존재하지 않는 수신자에게 선물을 보내면")
    public void 존재하지_않는_수신자에게_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "receiverId", 999999L,
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("수신자 ID 없이 선물을 보내면")
    public void 수신자_ID_없이_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", context.getSenderId())
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("Member-Id 헤더 없이 선물을 보내면")
    public void Member_Id_헤더_없이_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }

    @When("존재하지 않는 Member-Id로 선물을 보내면")
    public void 존재하지_않는_Member_Id로_선물을_보내면() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 999999L)
                .body(Map.of(
                        "optionId", context.getOptionId(),
                        "quantity", 1,
                        "receiverId", context.getReceiverId(),
                        "message", "선물입니다"
                ))
                .post("/api/gifts");
        context.setLastResponse(response);
    }
}
