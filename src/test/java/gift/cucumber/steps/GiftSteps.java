package gift.cucumber.steps;

import gift.cucumber.TestContext;
import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

public class GiftSteps {

    private final TestContext context;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;

    public GiftSteps(
            TestContext context,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            OptionRepository optionRepository,
            MemberRepository memberRepository
    ) {
        this.context = context;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
    }

    @조건("재고가 {int}개인 옵션이 등록되어 있다")
    public void 옵션이_등록되어_있다(int quantity) {
        Category category = categoryRepository.save(new Category("테스트카테고리"));
        Product product = productRepository.save(new Product("테스트상품", 10000, "https://example.com/img.png", category));
        Option option = optionRepository.save(new Option("테스트옵션", quantity, product));
        context.saveId("option", option.getId());
    }

    @조건("이름이 {string}인 보내는 사람이 등록되어 있다")
    public void 보내는_사람이_등록되어_있다(String name) {
        Member member = memberRepository.save(new Member(name, name + "@test.com"));
        context.saveId("sender", member.getId());
    }

    @조건("이름이 {string}인 받는 사람이 등록되어 있다")
    public void 받는_사람이_등록되어_있다(String name) {
        Member member = memberRepository.save(new Member(name, name + "@test.com"));
        context.saveId("receiver", member.getId());
    }

    @만일("보내는 사람이 받는 사람에게 {int}개를 {string} 메시지와 함께 선물하면")
    public void 선물하기_요청(int quantity, String message) {
        var response = RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", context.getId("sender"))
                    .body(Map.of(
                            "optionId", context.getId("option"),
                            "quantity", quantity,
                            "receiverId", context.getId("receiver"),
                            "message", message
                    ))
                .when()
                    .post("/api/gifts");

        context.setResponse(response);
    }

    @만일("보내는 사람이 받는 사람에게 다시 {int}개를 {string} 메시지와 함께 선물하면")
    public void 다시_선물하기_요청(int quantity, String message) {
        var response = RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", context.getId("sender"))
                    .body(Map.of(
                            "optionId", context.getId("option"),
                            "quantity", quantity,
                            "receiverId", context.getId("receiver"),
                            "message", message
                    ))
                .when()
                    .post("/api/gifts");

        context.setResponse(response);
    }

    @만일("존재하지 않는 옵션으로 선물하기를 요청하면")
    public void 존재하지_않는_옵션으로_선물하기() {
        var response = RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", context.getId("sender"))
                    .body(Map.of(
                            "optionId", 999,
                            "quantity", 1,
                            "receiverId", context.getId("receiver"),
                            "message", "생일 축하해!"
                    ))
                .when()
                    .post("/api/gifts");

        context.setResponse(response);
    }

    @만일("존재하지 않는 회원이 선물하기를 요청하면")
    public void 존재하지_않는_회원이_선물하기() {
        var response = RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 999L)
                    .body(Map.of(
                            "optionId", context.getId("option"),
                            "quantity", 1,
                            "receiverId", context.getId("receiver"),
                            "message", "생일 축하해!"
                    ))
                .when()
                    .post("/api/gifts");

        context.setResponse(response);
    }
}
