package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @먼저("{string} 상품에 재고 {int}개의 {string} 옵션이 존재한다")
    public void 옵션이_존재한다(String productName, int quantity, String optionName) {
        Long productId = context.get("productId:" + productName, Long.class);
        Product product = productRepository.findById(productId).orElseThrow();
        Option option = optionRepository.save(new Option(optionName, quantity, product));
        context.set("optionId:" + optionName, option.getId());
    }

    @먼저("{string} 회원이 존재한다")
    public void 회원이_존재한다(String memberName) {
        Member member = memberRepository.save(new Member(memberName, memberName + "@test.com"));
        context.set("memberId:" + memberName, member.getId());
    }

    @만일("{string}이 {string} 옵션 {int}개를 선물한다")
    public void 선물을_보낸다(String memberName, String optionName, int quantity) {
        Long memberId = context.get("memberId:" + memberName, Long.class);
        Long optionId = context.get("optionId:" + optionName, Long.class);

        Response response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", memberId)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts");

        context.set("lastResponse", response);
    }

    @만일("존재하지 않는 옵션으로 선물을 보낸다")
    public void 존재하지_않는_옵션으로_선물을_보낸다() {
        Long memberId = context.get("memberId:홍길동", Long.class);

        Response response = given()
            .contentType(ContentType.JSON)
            .header("Member-Id", memberId)
            .body(Map.of(
                "optionId", 9999L,
                "quantity", 3,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts");

        context.set("lastResponse", response);
    }

    @만일("회원 정보 없이 {string} 옵션 {int}개를 선물한다")
    public void 회원_정보_없이_선물한다(String optionName, int quantity) {
        Long optionId = context.get("optionId:" + optionName, Long.class);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", 2L,
                "message", "생일 축하해!"
            ))
        .when()
            .post("/api/gifts");

        context.set("lastResponse", response);
    }

    @그러면("선물 발송이 성공한다")
    public void 선물_발송이_성공한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(200);
    }

    @그러면("선물 발송이 실패한다")
    public void 선물_발송이_실패한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(500);
    }

    @그러면("재고 부족으로 실패한다")
    public void 재고_부족으로_실패한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(500);
    }

    @그러면("회원 정보 누락으로 실패한다")
    public void 회원_정보_누락으로_실패한다() {
        Response response = context.get("lastResponse", Response.class);
        response.then().statusCode(400);
    }

    @그리고("{string} 옵션의 재고가 {int}개이다")
    public void 옵션의_재고가_N개이다(String optionName, int expectedQuantity) {
        Long optionId = context.get("optionId:" + optionName, Long.class);
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
