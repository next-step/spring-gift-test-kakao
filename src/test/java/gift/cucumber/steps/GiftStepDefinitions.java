package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static gift.Fixtures.member;
import static gift.Fixtures.option;
import static gift.Fixtures.product;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    ScenarioContext context;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    MemberRepository memberRepository;

    @조건("{string} 카테고리가 있다")
    public void 카테고리가_있다(String categoryName) {
        var category = categoryRepository.save(new Category(categoryName));
        context.set("category:" + categoryName, category);
    }

    @조건("{string} 카테고리에 {string} 상품이 있다")
    public void 카테고리에_상품이_있다(String categoryName, String productName) {
        Category category = context.get("category:" + categoryName);
        var saved = productRepository.save(product(productName, category));
        context.set("product:" + productName, saved);
    }

    @조건("회원 {string}이 있다")
    public void 회원이_있다(String memberName) {
        var saved = memberRepository.save(member(memberName));
        context.set("member:" + memberName, saved);
    }

    @조건("{string} 상품에 {string} 옵션의 재고가 {int}개 있다")
    public void 옵션_재고_설정(String productName, String optionName, int quantity) {
        var saved = optionRepository.save(option(quantity, context.get("product:" + productName)));
        context.set("option:" + optionName, saved);
    }

    @만일("{string}이 {string}에게 {string} 옵션 {int}개를 선물한다")
    public void 선물을_보낸다(String senderName, String receiverName, String optionName, int quantity) {
        Member sender = context.get("member:" + senderName);
        Member receiver = context.get("member:" + receiverName);
        Option opt = context.get("option:" + optionName);

        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(opt.getId(), quantity, receiver.getId()))
                .when()
                .post("/api/gifts");

        context.setLastResponse(response);
    }

    @만일("{string}이 {string}에게 존재하지 않는 옵션으로 선물한다")
    public void 존재하지_않는_옵션으로_선물(String senderName, String receiverName) {
        Member sender = context.get("member:" + senderName);
        Member receiver = context.get("member:" + receiverName);

        var response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body("""
                        {
                            "optionId": 999999,
                            "quantity": 1,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(receiver.getId()))
                .when()
                .post("/api/gifts");

        context.setLastResponse(response);
    }

    @그러면("선물이 성공한다")
    public void 선물이_성공한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(200);
    }

    @그러면("선물이 실패한다")
    public void 선물이_실패한다() {
        assertThat(context.getLastResponse().statusCode()).isEqualTo(500);
    }

    @그리고("{string} 옵션의 재고가 {int}개이다")
    public void 옵션_재고_검증(String optionName, int expectedQuantity) {
        Option opt = context.get("option:" + optionName);
        var actual = optionRepository.findById(opt.getId()).orElseThrow();
        assertThat(actual.getQuantity()).isEqualTo(expectedQuantity);
    }
}
