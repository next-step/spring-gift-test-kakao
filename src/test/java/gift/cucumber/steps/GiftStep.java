package gift.cucumber.steps;

import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class GiftStep {

    @Autowired
    private SharedContext context;

    @When("보내는 회원이 받는 회원에게 초콜릿 옵션을 {int}개 선물한다")
    public void 초콜릿_옵션_선물(int quantity) {
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body(Map.of(
                        "optionId", 1L,
                        "quantity", quantity,
                        "receiverId", 2L,
                        "message", "선물합니다"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @When("보내는 회원이 받는 회원에게 커피 옵션을 {int}개 선물한다")
    public void 커피_옵션_선물(int quantity) {
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body(Map.of(
                        "optionId", 2L,
                        "quantity", quantity,
                        "receiverId", 2L,
                        "message", "선물합니다"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @When("보내는 회원이 받는 회원에게 존재하지 않는 옵션 {long}으로 {int}개 선물한다")
    public void 존재하지_않는_옵션_선물(long optionId, int quantity) {
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", 2L,
                        "message", "선물합니다"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();
        context.setResponse(response);
    }

    @When("Member-Id 헤더 없이 선물한다")
    public void 헤더_없이_선물한다() {
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", 1L,
                        "quantity", 1,
                        "receiverId", 2L,
                        "message", "선물합니다"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();
        context.setResponse(response);
    }
}
