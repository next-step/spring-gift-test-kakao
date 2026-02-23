package gift.acceptance.steps;

import gift.acceptance.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

public class GiftSteps {

    @Autowired
    private TestContext testContext;

    private Response lastGiftResponse;

    @먼저("재고가 {int}인 옵션을 생성한다")
    public void 옵션을_생성한다(int quantity) {
        Long optionId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "블랙", "quantity": %d, "productId": %d}
                        """, quantity, testContext.getProductId()))
                .post("/api/options")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setOptionId(optionId);
    }

    @먼저("보내는 회원을 생성한다")
    public void 보내는_회원을_생성한다() {
        Long senderId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "홍길동", "email": "hong@example.com"}
                        """)
                .post("/api/members")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setSenderId(senderId);
    }

    @먼저("받는 회원을 생성한다")
    public void 받는_회원을_생성한다() {
        Long receiverId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "김철수", "email": "kim@example.com"}
                        """)
                .post("/api/members")
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setReceiverId(receiverId);
    }

    @만일("{int}개의 선물을 보낸다")
    public void N개의_선물을_보낸다(int quantity) {
        lastGiftResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", testContext.getSenderId())
                .body(String.format("""
                        {"optionId": %d, "quantity": %d, "receiverId": %d, "message": "선물이에요"}
                        """, testContext.getOptionId(), quantity, testContext.getReceiverId()))
                .when()
                .post("/api/gifts");
        testContext.setLastStatusCode(lastGiftResponse.statusCode());
    }

    @그러면("응답 상태코드가 {int}이다")
    public void 응답_상태코드_확인(int expectedStatusCode) {
        lastGiftResponse.then()
                .statusCode(expectedStatusCode);
    }
}
