package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class TestContext {

    private Response response;
    private final Map<String, Long> createdIds = new HashMap<>();

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public void saveId(String key, Long id) {
        createdIds.put(key, id);
    }

    public Long getId(String key) {
        return createdIds.get(key);
    }
}
