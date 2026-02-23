package gift.cucumber.gift;

import gift.cucumber.common.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

public class GiftSteps {
    private final GiftApiClient giftApiClient;
    private final GiftRepositorySupport giftRepositorySupport;
    private final ScenarioContext scenarioContext;

    public GiftSteps(
        final GiftApiClient giftApiClient,
        final GiftRepositorySupport giftRepositorySupport,
        final ScenarioContext scenarioContext
    ) {
        this.giftApiClient = giftApiClient;
        this.giftRepositorySupport = giftRepositorySupport;
        this.scenarioContext = scenarioContext;
    }

    @Given("보내는 사람과 받는 사람이 등록되어 있다")
    public void 보내는_사람과_받는_사람이_등록되어_있다() {
        var sender = giftRepositorySupport.seedMember("보내는사람", "sender@test.com");
        var receiver = giftRepositorySupport.seedMember("받는사람", "receiver@test.com");
        scenarioContext.setSavedSenderId(sender.getId());
        scenarioContext.setSavedReceiverId(receiver.getId());
    }

    @Given("보내는 사람이 등록되어 있다")
    public void 보내는_사람이_등록되어_있다() {
        var sender = giftRepositorySupport.seedMember("보내는사람", "sender@test.com");
        scenarioContext.setSavedSenderId(sender.getId());
    }

    @Given("받는 사람이 등록되어 있다")
    public void 받는_사람이_등록되어_있다() {
        var receiver = giftRepositorySupport.seedMember("받는사람", "receiver@test.com");
        scenarioContext.setSavedReceiverId(receiver.getId());
    }

    @Given("카테고리에 {string} 상품이 등록되어 있다")
    public void 카테고리에_상품이_등록되어_있다(String name) {
        var product = giftRepositorySupport.seedProduct(name, scenarioContext.getSavedCategoryId());
        scenarioContext.setSavedProductId(product.getId());
    }

    @Given("상품에 재고가 {int}개인 {string}이 등록되어 있다")
    public void 상품에_재고가_N개인_옵션이_등록되어_있다(int quantity, String optionName) {
        var option = giftRepositorySupport.seedOption(optionName, quantity, scenarioContext.getSavedProductId());
        scenarioContext.setSavedOptionId(option.getId());
    }

    @When("보내는 사람이 다음 선물을 전송한다:")
    public void 보내는_사람이_다음_선물을_전송한다(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps(String.class, String.class).get(0);
        scenarioContext.setLastResponse(giftApiClient.sendGift(
            scenarioContext.getSavedSenderId(),
            scenarioContext.getSavedOptionId(),
            Integer.parseInt(row.get("quantity")),
            scenarioContext.getSavedReceiverId(),
            row.get("message")
        ));
    }

    @When("헤더 없이 선물을 전송한다")
    public void 헤더_없이_선물을_전송한다() {
        scenarioContext.setLastResponse(giftApiClient.sendGiftWithoutHeader());
    }

    @When("보내는 사람이 존재하지 않는 옵션으로 선물을 전송한다")
    public void 보내는_사람이_존재하지_않는_옵션으로_선물을_전송한다() {
        scenarioContext.setLastResponse(giftApiClient.sendGift(
            scenarioContext.getSavedSenderId(),
            9999L,
            1,
            1L,
            "선물"
        ));
    }

    @When("존재하지 않는 사람이 선물을 전송한다")
    public void 존재하지_않는_사람이_선물을_전송한다() {
        scenarioContext.setLastResponse(giftApiClient.sendGift(
            9999L,
            scenarioContext.getSavedOptionId(),
            1,
            scenarioContext.getSavedReceiverId(),
            "선물"
        ));
    }

    @Then("옵션의 재고가 {int}개이다")
    public void 옵션의_재고가_N개이다(int expectedQuantity) {
        giftRepositorySupport.assertOptionQuantity(scenarioContext.getSavedOptionId(), expectedQuantity);
    }
}
