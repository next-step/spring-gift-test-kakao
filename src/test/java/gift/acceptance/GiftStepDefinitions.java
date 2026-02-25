package gift.acceptance;

import gift.support.ApiClient;
import gift.support.ScenarioContext;
import gift.support.TestDataBuilder;
import io.cucumber.java.ko.조건;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
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

	@조건("{string} 옵션의 재고가 {int}개 있다")
	public void 옵션_재고_설정(String optionName, int quantity) {
		Long optionId = testDataBuilder.createOption(optionName, quantity);
		context.set(optionName, optionId);
	}

	@조건("{string} 회원이 존재한다")
	public void 회원_생성(String memberName) {
		Long memberId = testDataBuilder.createMember(memberName);
		context.set(memberName, memberId);
	}

	@만약("{string}가 {string}에게 {string} {int}개를 선물한다")
	public void 선물하기(String senderName, String receiverName, String optionName, int quantity) {
		Long senderId = context.get(senderName, Long.class);
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = context.get(optionName, Long.class);

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@만약("{string}가 {string}에게 존재하지 않는 옵션을 {int}개를 선물한다")
	public void 존재하지_않는_옵션_선물하기(String senderName, String receiverName, int quantity) {
		Long senderId = context.get(senderName, Long.class);
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = -1L;

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@만약("존재하지 않는 회원이 {string}에게 {string} {int}개를 선물한다")
	public void 존재하지_않는_회원_선물하기(String receiverName, String optionName, int quantity) {
		Long senderId = -1L;
		Long receiverId = context.get(receiverName, Long.class);
		Long optionId = context.get(optionName, Long.class);

		Response response = apiClient.sendGift(senderId, receiverId, optionId, quantity);
		context.set("response", response);
	}

	@그러면("선물 발송이 성공한다")
	public void 선물_발송_성공() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(200);
	}

	@그러면("선물 발송이 실패한다")
	public void 선물_발송_실패() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(500);
	}

}
