package gift.cucumber.steps.common;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {
    private Response lastResponse;
    private Long savedCategoryId;
    private Long savedProductId;
    private Long savedOptionId;
    private Long savedSenderId;
    private Long savedReceiverId;

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

    public Long getSavedProductId() {
        return savedProductId;
    }

    public void setSavedProductId(final Long savedProductId) {
        this.savedProductId = savedProductId;
    }

    public Long getSavedOptionId() {
        return savedOptionId;
    }

    public void setSavedOptionId(final Long savedOptionId) {
        this.savedOptionId = savedOptionId;
    }

    public Long getSavedSenderId() {
        return savedSenderId;
    }

    public void setSavedSenderId(final Long savedSenderId) {
        this.savedSenderId = savedSenderId;
    }

    public Long getSavedReceiverId() {
        return savedReceiverId;
    }

    public void setSavedReceiverId(final Long savedReceiverId) {
        this.savedReceiverId = savedReceiverId;
    }
}
