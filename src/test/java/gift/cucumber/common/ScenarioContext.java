package gift.cucumber.common;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {
    private Response lastResponse;
    private Long savedCategoryId;

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(final Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public Long getSavedCategoryId() {
        return savedCategoryId;
    }

    public void setSavedCategoryId(final Long savedCategoryId) {
        this.savedCategoryId = savedCategoryId;
    }
}
