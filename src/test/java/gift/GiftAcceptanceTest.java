package gift;

import static io.restassured.RestAssured.given;

import gift.application.request.GiveGiftRequest;
import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GiftAcceptanceTest extends AcceptanceTestSupport {

    final int existingOptionQuantity = 100;
    Member giftSender;
    Member giftReceiver;
    Category existingCategory;
    Product existingProduct;
    Option existingOption;

    @BeforeEach
    void setUp() {
        super.setupRestAssured();

        giftSender = super.addMember("Test gift sender", "sender@test.com");
        giftReceiver = super.addMember("Test gift receiver", "receiver@test.com");

        existingCategory = super.addCategory("Test category");
        existingProduct = super.addProduct(
                "Test product", 1000, "https://sample.com",
                existingCategory.getId()
        );

        existingOption = super.addOption(
                "Test option", existingOptionQuantity,
                existingProduct.getId()
        );
    }

    @AfterEach
    void tearDown() {
        super.initAll();
    }

    @Test
    @DisplayName("사용자가 선물을 보낸다")
    void giveGift() {
        // Given: 보내는 사람, 받는 사람, 상품, 옵션이 존재한다 (@BeforeEach에서 준비)

        // When: 선물 보내기 API를 호출한다
        GiveGiftRequest request = new GiveGiftRequest(
                existingOption.getId(), existingOptionQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then: 상태 코드 200
        response.then()
                .statusCode(200);
    }

    private static Response postGift(GiveGiftRequest request, Long memberIdHeader) {

        RequestSpecification requestSpecification = given()
                .contentType(ContentType.JSON)
                .body(request);

        if (memberIdHeader != null) {
            requestSpecification.header("Member-Id", memberIdHeader);
        }

        return requestSpecification
                .when()
                .post("/api/gifts");
    }

    @Test
    @DisplayName("선물 요청 시 옵션 id 가 제공되지 않으면 에러를 일으킨다.")
    void giveGiftWithNullOptionId() {
        // Given : 없음

        // When : optionId 가 누락된 체 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                null, existingOptionQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then : 에러 응답 (400)
        response.then()
                .statusCode(400);
    }

    @Test
    @DisplayName("선물 요청 시 존재하지 않는 옵션 id 가 제공되면 에러를 일으킨다.")
    void giveGiftWithNotExistingOptionId() {
        // Given : 존재하지 않는 옵션 ID
        Long notExistingOptionId = Long.MAX_VALUE;

        // When: 존재하지 않는 옵션 ID 로 선물 보내기를 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                notExistingOptionId, existingOptionQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then : 에러 응답 (404)
        response.then()
                .statusCode(404);
    }

    @Test
    @DisplayName("선물 요청 시 수신자 id 가 제공되지 않으면 에러가 발생한다.")
    void giveGiftWithNullReceiverId() {
        // Given : 없음

        // When : receiverId 가 누락된 체 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                existingOption.getId(), existingOptionQuantity,
                null, "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then : 에러 응답 (400)
        response.then()
                .statusCode(400);
    }

    @Test
    @DisplayName("선물 요청 시 존재하지 않는 수신자 id 가 제공되면 에러가 발생한다.")
    void giveGiftWithNotExistingReceiverId() {
        // Given : 존재하지 않는 수신자 ID
        Long notExistingReceiverId = Long.MAX_VALUE;

        // When: 존재하지 않는 수신자 ID 로 선물 보내기를 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                existingOption.getId(), existingOptionQuantity,
                notExistingReceiverId, "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then : 에러 응답 (404)
        response.then()
                .statusCode(404);
    }

    @Test
    @DisplayName("선물 요청 시 Member-Id 헤더가 누락되면 에러가 발생한다.")
    void giveGiftWitNullMemberIdHeader() {
        // Given : 없음

        // When : Member-ID 헤더가 누락된 체 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                existingOption.getId(), existingOptionQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, null);

        // Then : 에러 응답 (400)
        response.then()
                .statusCode(400);
    }

    @Test
    @DisplayName("선물 요청 시 존재하지 않는 Member-Id 가 제공되면 에러가 발생한다.")
    void giveGiftWitNotExistingMemberIdHeader() {
        // Given : 존재하지 않는 Member-Id
        Long notExistingMemberId = Long.MAX_VALUE;

        // When: 존재하지 않는 Member-Id 로 선물 보내기를 요청한다.
        GiveGiftRequest request = new GiveGiftRequest(
                existingOption.getId(), existingOptionQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, notExistingMemberId);

        // Then : 에러 응답 (404)
        response.then()
                .statusCode(404);
    }

    @Test
    @DisplayName("남아있는 재고보다 더 많은 양을 선물하면 에러가 발생한다.")
    void giveGiftWithTooManyOptionQuantity() {
        // Given: 옵션의 재고는 10 개
        int quantity = 10;
        Option insufficientQuantityOption = super.addOption(
                "insufficient quantity", quantity, existingProduct.getId()
        );

        // When: 재고보다 많은 수량(100개)으로 선물 보내기를 요청한다
        int requestQuantity = 100;

        GiveGiftRequest request = new GiveGiftRequest(
                insufficientQuantityOption.getId(), requestQuantity,
                giftReceiver.getId(), "Test message"
        );
        Response response = postGift(request, giftSender.getId());

        // Then : 에러 응답 (500)
        response.then()
                .statusCode(500);
    }
}
