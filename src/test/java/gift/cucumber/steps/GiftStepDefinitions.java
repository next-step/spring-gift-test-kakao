package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.*;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @조건("보내는 회원과 받는 회원이 존재한다")
    public void 보내는_회원과_받는_회원이_존재한다() {
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        context.set("senderId", sender.getId());
        context.set("receiverId", receiver.getId());
    }

    @조건("{string} 옵션의 재고가 {int}개 있다")
    public void 옵션의_재고가_N개_있다(String optionName, int quantity) {
        Category category = categoryRepository.save(new Category("테스트카테고리"));
        Product product = productRepository.save(new Product("테스트상품", 10000, "http://image.url", category));
        Option option = optionRepository.save(new Option(optionName, quantity, product));
        context.set("optionId", option.getId());
        context.set("optionName", optionName);
    }

    @만일("회원이 {string} {int}개를 선물한다")
    public void 회원이_N개를_선물한다(String optionName, int quantity) {
        Long senderId = context.get("senderId", Long.class);
        Long receiverId = context.get("receiverId", Long.class);
        Long optionId = context.get("optionId", Long.class);

        Response response = RestAssured.given()
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

        context.set("lastResponse", response);
    }

    @만일("존재하지 않는 옵션으로 선물을 시도한다")
    public void 존재하지_않는_옵션으로_선물을_시도한다() {
        Long senderId = context.get("senderId", Long.class);
        Long receiverId = context.get("receiverId", Long.class);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", 9999,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        context.set("lastResponse", response);
    }

    @만일("존재하지 않는 회원이 {string} {int}개를 선물한다")
    public void 존재하지_않는_회원이_N개를_선물한다(String optionName, int quantity) {
        Long receiverId = context.get("receiverId", Long.class);
        Long optionId = context.get("optionId", Long.class);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 9999)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");

        context.set("lastResponse", response);
    }

    @그러면("선물하기가 성공한다")
    public void 선물하기가_성공한다() {
        Response response = context.get("lastResponse", Response.class);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @그러면("선물하기가 실패한다")
    public void 선물하기가_실패한다() {
        Response response = context.get("lastResponse", Response.class);
        assertThat(response.statusCode()).isEqualTo(500);
    }

    @그리고("{string} 옵션의 재고가 {int}개이다")
    public void 옵션의_재고가_N개이다(String optionName, int expectedQuantity) {
        Long optionId = context.get("optionId", Long.class);
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
