package gift.cucumber.common;

import io.cucumber.java.en.Then;

public class CommonSteps {
    private final ScenarioContext scenarioContext;

    public CommonSteps(final ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는_N이다(int statusCode) {
        scenarioContext.getLastResponse().then().statusCode(statusCode);
    }
}
