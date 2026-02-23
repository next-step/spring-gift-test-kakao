package gift.cucumber;

import io.cucumber.java.ko.그러면;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    private final ScenarioContext scenarioContext;

    public CommonStepDefinitions(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @그러면("응답 상태코드는 {int}이다")
    public void 응답_상태코드_검증(int statusCode) {
        assertThat(scenarioContext.getResponse().statusCode()).isEqualTo(statusCode);
    }
}
