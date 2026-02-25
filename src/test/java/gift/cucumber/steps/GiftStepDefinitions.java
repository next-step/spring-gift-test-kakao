package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.Member;
import gift.model.Option;
import gift.support.MemberFixture;
import gift.support.OptionFixture;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class GiftStepDefinitions {

    private final ScenarioContext context;
    private final OptionFixture optionFixture;
    private final MemberFixture memberFixture;

    public GiftStepDefinitions(ScenarioContext context, OptionFixture optionFixture, MemberFixture memberFixture) {
        this.context = context;
        this.optionFixture = optionFixture;
        this.memberFixture = memberFixture;
    }

    @Given("{string} 옵션의 재고가 {int}개 있다")
    public void 옵션의_재고가_있다(String optionName, int quantity) {
        Long productId = context.get("productId", Long.class);
        Option option = optionFixture.builder()
                .name(optionName)
                .quantity(quantity)
                .productId(productId)
                .build();
        context.set("optionId", option.getId());
    }

    @Given("회원이 존재한다")
    public void 회원이_존재한다() {
        Member sender = memberFixture.builder()
                .name("보내는사람")
                .email("sender@test.com")
                .build();
        Member receiver = memberFixture.builder()
                .name("받는사람")
                .email("receiver@test.com")
                .build();
        context.set("senderId", sender.getId());
        context.set("receiverId", receiver.getId());
    }

    @When("회원이 {string} 옵션 {int}개를 선물한다")
    public void 회원이_옵션을_선물한다(String optionName, int quantity) {
        Long optionId = context.get("optionId", Long.class);
        Long senderId = context.get("senderId", Long.class);
        Long receiverId = context.get("receiverId", Long.class);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts");
        context.set("response", response);
    }

    @When("존재하지 않는 옵션으로 선물한다")
    public void 존재하지_않는_옵션으로_선물한다() {
        Long senderId = context.get("senderId", Long.class);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", 999999,
                        "quantity", 1,
                        "receiverId", 999999,
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts");
        context.set("response", response);
    }

    @Then("선물 발송이 성공한다")
    public void 선물_발송이_성공한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(200);
    }

    @Then("재고 부족으로 실패한다")
    public void 재고_부족으로_실패한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(500);
    }

    @Then("선물 발송이 실패한다")
    public void 선물_발송이_실패한다() {
        Response response = context.get("response", Response.class);
        response.then()
                .statusCode(500);
    }
}
