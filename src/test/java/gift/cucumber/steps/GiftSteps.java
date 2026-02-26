package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import gift.cucumber.StubGiftDelivery;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static io.restassured.RestAssured.given;

public class GiftSteps {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StubGiftDelivery stubGiftDelivery;

    private final ScenarioContext scenarioContext;

    public GiftSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("선물 테스트 데이터가 준비되어 있다")
    public void 선물_테스트_데이터가_준비되어_있다() {
        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("sql/category-data.sql"));
        populator.addScript(new ClassPathResource("sql/product-data.sql"));
        populator.addScript(new ClassPathResource("sql/option-data.sql"));
        populator.addScript(new ClassPathResource("sql/member-data.sql"));
        populator.execute(dataSource);
    }

    @Given("배송 시스템이 장애 상태다")
    public void 배송_시스템이_장애_상태다() {
        stubGiftDelivery.setShouldFail(true);
    }

    @When("회원 {long}이 옵션 {long}을 {int}개 회원 {long}에게 선물하면")
    public void 회원이_옵션을_개_회원에게_선물하면(long senderId, long optionId, int quantity, long receiverId) {
        String body = """
                {
                    "optionId": %d,
                    "quantity": %d,
                    "receiverId": %d,
                    "message": "테스트 선물"
                }
                """.formatted(optionId, quantity, receiverId);

        var response = given()
                .header("Member-Id", senderId)
                .contentType("application/json")
                .body(body)
        .when()
                .post("/api/gifts");
        scenarioContext.setLastResponse(response);
    }
}
