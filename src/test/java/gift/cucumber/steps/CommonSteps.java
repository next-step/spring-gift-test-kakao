package gift.cucumber.steps;

import gift.cucumber.CucumberSpringConfiguration;
import gift.cucumber.DatabaseCleaner;
import gift.cucumber.ScenarioContext;
import gift.cucumber.StubGiftDelivery;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonSteps {

    @Autowired
    private CucumberSpringConfiguration springConfiguration;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private StubGiftDelivery stubGiftDelivery;

    @Before
    public void setUp() {
        RestAssured.port = springConfiguration.getPort();
        databaseCleaner.clear();
        scenarioContext.clear();
        stubGiftDelivery.reset();
    }

    @Then("응답 상태 코드가 {int}이다")
    public void 응답_상태_코드가_이다(int statusCode) {
        scenarioContext.getLastResponse().then().statusCode(statusCode);
    }
}
