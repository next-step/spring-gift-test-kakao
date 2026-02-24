package gift.cucumber.steps;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class SharedContext {

    private ExtractableResponse<Response> response;
    private long savedCategoryId;

    public ExtractableResponse<Response> getResponse() {
        return response;
    }

    public void setResponse(ExtractableResponse<Response> response) {
        this.response = response;
    }

    public long getSavedCategoryId() {
        return savedCategoryId;
    }

    public void setSavedCategoryId(long savedCategoryId) {
        this.savedCategoryId = savedCategoryId;
    }
}
