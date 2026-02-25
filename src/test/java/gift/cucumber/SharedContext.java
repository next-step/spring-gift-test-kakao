package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class SharedContext {
    private Response lastResponse;
    private final Map<String, Long> resourceIds = new HashMap<>();

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }

    public void putId(String key, Long id) {
        resourceIds.put(key, id);
    }

    public Long getId(String key) {
        return resourceIds.get(key);
    }
}
