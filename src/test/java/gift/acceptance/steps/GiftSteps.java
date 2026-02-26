package gift.acceptance.steps;

import gift.acceptance.ApiClient;
import gift.acceptance.ScenarioContext;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext context;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Given("{string} 옵션이 재고 {int}개로 등록되어 있다")
    public void 옵션이_등록되어_있다(String name, int quantity) {
        Product product = productRepository.findById(context.getProductId()).orElseThrow();
        Option option = optionRepository.save(new Option(name, quantity, product));
        context.setOptionId(option.getId());
    }

    @Given("보내는 회원과 받는 회원이 등록되어 있다")
    public void 회원이_등록되어_있다() {
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        context.setSenderId(sender.getId());
        context.setReceiverId(receiver.getId());
    }

    @When("보내는 회원이 {int}개 수량으로 선물을 전송하면")
    public void 선물을_전송하면(int quantity) {
        ExtractableResponse<Response> response = apiClient.post("/api/gifts",
                Map.of("optionId", context.getOptionId(), "quantity", quantity,
                        "receiverId", context.getReceiverId(), "message", "선물입니다"),
                Map.of("Member-Id", context.getSenderId()));
        context.setResponse(response);
    }

    @When("보내는 회원이 존재하지 않는 옵션으로 선물을 전송하면")
    public void 존재하지_않는_옵션으로_선물을_전송하면() {
        ExtractableResponse<Response> response = apiClient.post("/api/gifts",
                Map.of("optionId", 99999, "quantity", 1,
                        "receiverId", context.getReceiverId(), "message", "선물입니다"),
                Map.of("Member-Id", context.getSenderId()));
        context.setResponse(response);
    }

    // Member-Id 헤더 없이 요청하여 인증되지 않은 사용자를 시뮬레이션
    @When("로그인하지 않은 사용자가 선물을 전송하면")
    public void 로그인하지_않은_사용자가_선물을_전송하면() {
        ExtractableResponse<Response> response = apiClient.post("/api/gifts",
                Map.of("optionId", context.getOptionId(), "quantity", 1,
                        "receiverId", context.getReceiverId(), "message", "선물입니다"));
        context.setResponse(response);
    }

    // HTTP 4xx/5xx 응답을 실패로 판단 (재고 부족: 500, 인증 실패: 400, 옵션 미존재: 500)
    @Then("선물 전송에 실패한다")
    public void 선물_전송에_실패한다() {
        assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Then("옵션 재고가 {int}개로 차감되어 있다")
    public void 옵션_재고가_차감되어_있다(int expectedQuantity) {
        Option option = optionRepository.findById(context.getOptionId()).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }

    @Then("옵션 재고가 {int}개로 변경되지 않았다")
    public void 옵션_재고가_변경되지_않았다(int expectedQuantity) {
        Option option = optionRepository.findById(context.getOptionId()).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
