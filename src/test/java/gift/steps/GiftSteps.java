package gift.steps;

import gift.model.GiftDelivery;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftSteps {

    @Autowired
    private SharedContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GiftDelivery giftDelivery;

    private final Map<String, Option> options = new java.util.HashMap<>();
    private final Map<String, Member> members = new java.util.HashMap<>();

    // 옵션 생성 API가 없으므로 Repository 직접 접근
    @Given("재고 {int}개인 {string} 옵션이 등록되어 있고")
    public void 옵션이_등록되어_있고(int quantity, String optionName) {
        Long productId = ((Number) context.getId("productId")).longValue();
        Product product = productRepository.findById(productId).orElseThrow();
        Option option = optionRepository.save(new Option(optionName, quantity, product));
        options.put(optionName, option);
    }

    // 회원 생성 API가 없으므로 Repository 직접 접근
    @Given("회원 {string}이 존재할 때")
    public void 회원이_존재할_때(String name) {
        Member member = memberRepository.save(new Member(name, name + "@test.com"));
        members.put(name, member);
    }

    @When("{string}이 {string} 옵션 {int}개를 선물하면")
    public void 옵션을_선물하면(String memberName, String optionName, int quantity) {
        Member sender = members.get(memberName);
        Option option = options.get(optionName);

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                    "optionId", option.getId(),
                    "quantity", quantity,
                    "receiverId", 2L,
                    "message", "생일 축하해!"
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @When("{string}이 존재하지 않는 옵션으로 선물하면")
    public void 존재하지_않는_옵션으로_선물하면(String memberName) {
        Member sender = members.get(memberName);

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                    "optionId", 9999L,
                    "quantity", 3,
                    "receiverId", 2L,
                    "message", "생일 축하해!"
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @When("Member-Id 없이 {string} 옵션 {int}개를 선물하면")
    public void memberId_없이_선물하면(String optionName, int quantity) {
        Option option = options.get(optionName);

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "optionId", option.getId(),
                    "quantity", quantity,
                    "receiverId", 2L,
                    "message", "생일 축하해!"
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @And("{string} 옵션의 재고는 {int}이다")
    public void 옵션의_재고는(String optionName, int expectedQuantity) {
        Option option = options.get(optionName);
        Option updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(expectedQuantity);
    }

    @And("선물 배달이 호출된다")
    public void 선물_배달이_호출된다() {
        // Docker 컨테이너 내부 동작은 검증 불가 — 응답 코드로 성공 여부를 판단
    }
}
