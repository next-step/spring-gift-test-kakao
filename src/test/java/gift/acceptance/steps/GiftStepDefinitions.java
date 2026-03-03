package gift.acceptance.steps;

import gift.acceptance.ScenarioContext;
import gift.model.Option;
import gift.model.OptionRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ScenarioContext context;

    @만일("회원 {long}이 옵션 {long}로 수량 {int}의 선물을 회원 {long}에게 보낸다")
    public void 회원이_옵션으로_선물을_보낸다(long memberId, long optionId, int quantity, long receiverId) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts"));
    }

    @만일("Member-Id 헤더 없이 옵션 {long}로 수량 {int}의 선물을 회원 {long}에게 보낸다")
    public void Member_Id_헤더_없이_선물을_보낸다(long optionId, int quantity, long receiverId) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts"));
    }

    @만일("Member-Id가 {string}인 회원이 옵션 {long}로 수량 {int}의 선물을 회원 {long}에게 보낸다")
    public void Member_Id가_문자열인_회원이_선물을_보낸다(String memberId, long optionId, int quantity, long receiverId) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts"));
    }

    @만일("회원 {long}이 잘못된 형식의 선물 요청을 보낸다")
    public void 잘못된_형식의_선물_요청을_보낸다(long memberId) {
        context.setResponse(RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body("\"invalid json\"")
                .when()
                .post("/api/gifts"));
    }

    @그리고("옵션 {long}의 재고는 {int}이다")
    public void 옵션의_재고는_이다(long optionId, int expectedQuantity) {
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
