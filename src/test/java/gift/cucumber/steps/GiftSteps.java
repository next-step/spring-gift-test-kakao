package gift.cucumber.steps;

import gift.cucumber.TestContext;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class GiftSteps {

    @Value("${cucumber.target.url}")
    private String targetUrl;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestContext testContext;

    @그리고("상품에 재고 {int}개의 {string}이 등록되어 있다")
    public void 상품에_재고_n개의_옵션이_등록되어_있다(int quantity, String optionName) {
        Option option = optionRepository.save(
                new Option(optionName, quantity, testContext.getLastProduct())
        );
        testContext.setLastOption(option);
    }

    @그리고("{string} 회원이 존재한다")
    public void 회원이_존재한다(String name) {
        Member member = memberRepository.save(new Member(name, name + "@test.com"));
        testContext.addMember(name, member);
    }

    @만일("{string}이 {string}에게 {int}개를 선물하면")
    public void 이_에게_n개를_선물하면(String senderName, String receiverName, int quantity) {
        Member sender = testContext.getMember(senderName);
        Member receiver = testContext.getMember(receiverName);
        Option option = testContext.getLastOption();

        String body = String.format("""
                {
                    "optionId": %d,
                    "quantity": %d,
                    "receiverId": %d,
                    "message": "생일 축하해!"
                }
                """, option.getId(), quantity, receiver.getId());

        Response response = RestAssured.given()
                .baseUri(targetUrl)
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(body)
                .when()
                .post("/api/gifts");
        testContext.setLastResponse(response);
    }
}
