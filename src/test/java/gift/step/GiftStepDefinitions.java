package gift.step;

import static io.restassured.RestAssured.given;

import gift.TestDataManipulator;
import gift.application.request.GiveGiftRequest;
import gift.model.Member;
import gift.model.Option;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

/**
 * 선물하기 관련 Step Definition.
 * <p>
 * 회원·옵션 생성 Given step과 선물 보내기 When step을 포함한다. 선물 API는 {@code Member-Id} 헤더로 발신자를 식별하므로, 헤더 유무에 따른
 * 에러 시나리오도 여기서 처리한다.
 */
public class GiftStepDefinitions {

    private final AcceptanceContext context;
    private final TestDataManipulator dataManipulator;

    public GiftStepDefinitions(AcceptanceContext context, TestDataManipulator dataManipulator) {
        this.context = context;
        this.dataManipulator = dataManipulator;
    }

    // --- Given ---

    @Given("회원 {string}가 등록되어 있다.")
    public void memberExistsWithDefaultEmail(String name) {
        memberExists(name, name.toLowerCase() + "@test.com");
    }

    @Given("회원 {string}가 {string} 이메일로 등록되어 있다.")
    public void memberExists(String name, String email) {
        Member member = dataManipulator.addMember(name, email);

        context.putMemberId(name, member.getId());
    }

    @Given("상품의 옵션 {string}이 등록되어 있다.")
    public void optionExistsWithDefaultQuantity(String name) {
        optionExists(name, 100);
    }

    @Given("상품의 옵션 {string}이 재고 {int}으로 등록되어 있다.")
    public void optionExists(String name, int quantity) {
        Long productId = context.getLastProductId();
        Option option = dataManipulator.addOption(name, quantity, productId);

        context.putOptionId(name, option.getId());
    }

    // --- When: 정상 선물 ---

    @When("{string}가 {string}에게 옵션 {string} {int}개를 선물한다")
    public void sendGift(String sender, String receiver, String optionName, int quantity) {
        sendGiftWithMessage(sender, receiver, optionName, quantity, "테스트 선물");
    }

    @When("{string}가 {string}에게 옵션 {string} {int}개를 {string} 메세지로 선물한다")
    public void sendGiftWithMessage(
            String sender, String receiver, String optionName, int quantity, String message
    ) {
        Long senderId = context.getMemberId(sender);
        Long receiverId = context.getMemberId(receiver);
        Long optionId = context.getOptionId(optionName);

        GiveGiftRequest request = new GiveGiftRequest(
                optionId, quantity, receiverId, message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", senderId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    // --- When: 옵션 관련 에러 ---

    @When("{string}가 {string}에게 옵션 id 없이 선물 보내기를 요청한다")
    public void sendGiftWithoutOptionId(String sender, String receiver) {
        sendGiftWithoutOptionIdWithMessage(sender, receiver, "테스트 선물");
    }

    @When("{string}가 {string}에게 {string} 메세지로 옵션 id 없이 선물 보내기를 요청한다")
    public void sendGiftWithoutOptionIdWithMessage(String sender, String receiver, String message) {
        Long senderId = context.getMemberId(sender);
        Long receiverId = context.getMemberId(receiver);

        GiveGiftRequest request = new GiveGiftRequest(
                null, 1, receiverId, message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", senderId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    @When("{string}가 {string}에게 존재하지 않는 옵션 id로 선물 보내기를 요청한다")
    public void sendGiftWithNonExistentOptionId(String sender, String receiver) {
        sendGiftWithNonExistentOptionIdWithMessage(sender, receiver, "테스트 선물");
    }

    @When("{string}가 {string}에게 {string} 메세지로 존재하지 않는 옵션 id로 선물 보내기를 요청한다")
    public void sendGiftWithNonExistentOptionIdWithMessage(
            String sender, String receiver, String message
    ) {
        Long senderId = context.getMemberId(sender);
        Long receiverId = context.getMemberId(receiver);
        Long notExistingId = context.getNotExistingId();

        GiveGiftRequest request = new GiveGiftRequest(
                notExistingId, 1, receiverId, message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", senderId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    // --- When: 수신자 관련 에러 ---

    @When("수신자 id 없이 {string}가 {string} 메세지로 옵션 {string}를 재고 {int} 만큼 선물 보내기를 요청한다")
    public void sendGiftWithoutReceiverId(
            String sender, String message, String option, int quantity
    ) {
        Long senderId = context.getMemberId(sender);
        Long optionId = context.getOptionId(option);

        GiveGiftRequest request = new GiveGiftRequest(
                optionId, quantity, null,
                message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", senderId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    @When("존재하지 않는 수신자 id로 {string}가 {string} 메세지로 옵션 {string}를 재고 {int} 만큼 선물 보내기를 요청한다")
    public void sendGiftWithNonExistentReceiverId(
            String sender, String message, String option, int quantity
    ) {
        Long senderId = context.getMemberId(sender);
        Long optionId = context.getOptionId(option);
        Long notExistingId = context.getNotExistingId();

        GiveGiftRequest request = new GiveGiftRequest(
                optionId, quantity, notExistingId,
                message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", senderId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    // --- When: Member-Id 헤더 관련 에러 ---

    @When("Member-Id 헤더 없이 {string}에게 {string} 메세지로 옵션 {string}을 {int} 만큼 선물 보내기를 요청한다")
    public void sendGiftWithoutMemberIdHeader(
            String receiver, String message, String option, int quantity
    ) {
        Long receiverId = context.getMemberId(receiver);
        Long optionId = context.getOptionId(option);

        GiveGiftRequest request = new GiveGiftRequest(
                optionId, quantity, receiverId, message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }

    @When("존재하지 않는 Member-Id로 {string}에게 {string} 메세지로 옵션 {string}을 {int} 만큼 선물 보내기를 요청한다")
    public void sendGiftWithNonExistentMemberId(
            String receiver, String message, String option, int quantity
    ) {
        Long receiverId = context.getMemberId(receiver);
        Long optionId = context.getOptionId(option);

        Long notExistingId = context.getNotExistingId();

        GiveGiftRequest request = new GiveGiftRequest(
                optionId, quantity, receiverId,
                message
        );

        context.setResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Member-Id", notExistingId)
                        .body(request)
                        .when()
                        .post("/api/gifts")
        );
    }
}
