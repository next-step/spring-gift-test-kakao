package gift.acceptance;

import static org.assertj.core.api.Assertions.*;

import gift.support.ApiClient;
import gift.support.ScenarioContext;
import gift.support.TestDataBuilder;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

public class GiftStepDefinitions {

	private final ScenarioContext context;
	private final TestDataBuilder testDataBuilder;
	private final ApiClient apiClient;
	public GiftStepDefinitions(ScenarioContext context, TestDataBuilder testDataBuilder, ApiClient apiClient) {
		this.context = context;
		this.testDataBuilder = testDataBuilder;
		this.apiClient = apiClient;
	}

	@Given("{string} 옵션의 재고가 {int}개 있다")
	public void 옵션_재고_설정(String optionName, int quantity) {
		Long optionId = testDataBuilder.createOption(optionName, quantity);
		context.set(optionName, optionId);
	}

	@Given("{string} 회원이 존재한다")
	public void 회원_생성(String memberName) {
		Long memberId = testDataBuilder.createMember(memberName);
		context.set(memberName, memberId);
	}

	@When("{string}가 {string}에게 {string} {int}개를 선물한다")
	public void 선물하기(String senderName, String receiverName, String optionName, int quantity) {
		Long senderId = context.get(senderName, Long.class);
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = context.get(optionName, Long.class);

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@When("{string}가 {string}에게 존재하지 않는 옵션을 {int}개를 선물한다")
	public void 존재하지_않는_옵션_선물하기(String senderName, String receiverName, int quantity) {
		Long senderId = context.get(senderName, Long.class);
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = -1L;

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@When("존재하지 않는 회원이 {string}에게 {string} {int}개를 선물한다")
	public void 존재하지_않는_회원_선물하기(String receiverName, String optionName, int quantity) {
		Long senderId = -1L;
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = context.get(optionName, Long.class);

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@Then("선물 발송이 성공한다")
	public void 선물_발송_성공() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(200);
	}

	@Then("선물 발송이 실패한다")
	public void 선물_발송_실패() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(500);
	}

	@Then("{string} 옵션의 재고가 {int}개이다")
	public void 옵션_재고_검증(String optionName, int expectedQuantity) {
		Long optionId = context.get(optionName, Long.class);
		int actualQuantity = testDataBuilder.getOptionStock(optionId);

		assertThat(actualQuantity).isEqualTo(expectedQuantity);
	}
}
