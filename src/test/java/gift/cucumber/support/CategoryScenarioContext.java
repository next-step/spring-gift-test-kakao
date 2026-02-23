package gift.cucumber.support;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class CategoryScenarioContext {
    private Response lastResponse;

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(final Response lastResponse) {
        this.lastResponse = lastResponse;
    }
}
