package steps;

import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftSteps {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final ScenarioContext context;

    public GiftSteps(ScenarioContext context, MemberRepository memberRepository,
                     ProductRepository productRepository, OptionRepository optionRepository) {
        this.context = context;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    @Given("이름이 {string} 이메일이 {string}인 회원이 등록되어 있다")
    public void 회원_등록(String name, String email) {
        memberRepository.save(new Member(name, email));
    }

    @Given("상품 {string}에 이름 {string} 재고 {int}인 옵션이 등록되어 있다")
    public void 옵션_등록(String productName, String optionName, int quantity) {
        var productId = given()
        .when()
            .get("/api/products")
        .then()
            .extract()
            .jsonPath()
            .getList("findAll { it.name == '" + productName + "' }.id", Long.class)
            .get(0);
        var product = productRepository.findById(productId).orElseThrow();
        optionRepository.save(new Option(optionName, quantity, product));
    }

    @When("회원 {string}이 회원 {string}에게 옵션 {string} 수량 {int} 메시지 {string}로 선물을 전송하면")
    public void 선물_전송(String senderName, String receiverName, String optionName, int quantity, String message) {
        var sender = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(senderName))
            .findFirst().orElseThrow();
        var receiver = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(receiverName))
            .findFirst().orElseThrow();
        var option = optionRepository.findAll().stream()
            .filter(o -> o.getName().equals(optionName))
            .findFirst().orElseThrow();

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                    "optionId", option.getId(),
                    "quantity", quantity,
                    "receiverId", receiver.getId(),
                    "message", message
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @When("Member-Id 헤더 없이 선물을 전송하면")
    public void 헤더_없이_선물_전송() {
        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "optionId", 1L,
                    "quantity", 1,
                    "receiverId", 1L,
                    "message", "선물"
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @When("회원 {string}이 존재하지 않는 옵션으로 선물을 전송하면")
    public void 존재하지_않는_옵션으로_선물_전송(String senderName) {
        var sender = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(senderName))
            .findFirst().orElseThrow();

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                    "optionId", 9999L,
                    "quantity", 1,
                    "receiverId", 1L,
                    "message", "선물"
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @When("존재하지 않는 회원이 회원 {string}에게 옵션 {string} 수량 {int} 메시지 {string}로 선물을 전송하면")
    public void 미존재_회원이_선물_전송(String receiverName, String optionName, int quantity, String message) {
        var receiver = memberRepository.findAll().stream()
            .filter(m -> m.getName().equals(receiverName))
            .findFirst().orElseThrow();
        var option = optionRepository.findAll().stream()
            .filter(o -> o.getName().equals(optionName))
            .findFirst().orElseThrow();

        context.setResponse(
            given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 9999L)
                .body(Map.of(
                    "optionId", option.getId(),
                    "quantity", quantity,
                    "receiverId", receiver.getId(),
                    "message", message
                ))
            .when()
                .post("/api/gifts")
        );
    }

    @Then("옵션 {string}의 재고는 {int}이다")
    public void 옵션_재고_확인(String optionName, int expectedQuantity) {
        var option = optionRepository.findAll().stream()
            .filter(o -> o.getName().equals(optionName))
            .findFirst().orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
