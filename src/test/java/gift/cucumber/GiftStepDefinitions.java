package gift.cucumber;

import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.그러면;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private SharedContext sharedContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @먼저("{string} 회원이 등록되어 있다")
    public void 회원이_등록되어_있다(String name) {
        var member = memberRepository.save(new Member(name, name + "@test.com"));
        sharedContext.putId("회원_" + name, member.getId());
    }

    @먼저("{string} 상품에 재고 {int}개인 {string} 옵션이 등록되어 있다")
    public void 옵션이_등록되어_있다(String productName, int quantity, String optionName) {
        long productId = sharedContext.getId("상품_" + productName);
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        var option = optionRepository.save(new Option(optionName, quantity, product));
        sharedContext.putId("옵션_" + optionName, option.getId());
    }

    @만일("{string}이 {string}에게 {string} 옵션을 {int}개 선물한다")
    public void 선물한다(String senderName, String receiverName, String optionName, int quantity) {
        long senderId = sharedContext.getId("회원_" + senderName);
        long optionId = sharedContext.getId("옵션_" + optionName);
        long receiverId = sharedContext.getId("회원_" + receiverName);
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(optionId, quantity, receiverId))
                .when()
                .post("/api/gifts");
        sharedContext.setLastResponse(response);
    }

    @만일("{string}이 존재하지 않는 옵션을 선물한다")
    public void 존재하지_않는_옵션을_선물한다(String senderName) {
        long senderId = sharedContext.getId("회원_" + senderName);
        long receiverId = sharedContext.getId("회원_받는사람");
        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                        {
                            "optionId": 999999,
                            "quantity": 1,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(receiverId))
                .when()
                .post("/api/gifts");
        sharedContext.setLastResponse(response);
    }

    @그러면("선물 보내기가 성공한다")
    public void 선물_보내기가_성공한다() {
        sharedContext.getLastResponse().then().statusCode(200);
    }

    @그러면("선물 보내기가 실패한다")
    public void 선물_보내기가_실패한다() {
        int statusCode = sharedContext.getLastResponse().then().extract().statusCode();
        assertThat(statusCode).isGreaterThanOrEqualTo(400);
    }

    @그러면("{string} 옵션의 재고는 {int}개이다")
    public void 옵션의_재고_검증(String optionName, int expectedQuantity) {
        long optionId = sharedContext.getId("옵션_" + optionName);
        var option = optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Option not found: " + optionId));
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
