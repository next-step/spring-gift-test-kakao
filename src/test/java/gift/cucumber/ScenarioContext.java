package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {
    private int responseStatusCode;

    public int getResponseStatusCode() {
        return responseStatusCode;
    }

    public void setResponseStatusCode(int responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }
}
