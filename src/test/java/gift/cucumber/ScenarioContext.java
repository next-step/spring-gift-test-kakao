package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private Response lastResponse;
    private final Map<String, Object> storage = new HashMap<>();

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public void put(String key, Object value) {
        storage.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) storage.get(key);
    }

    public void clear() {
        lastResponse = null;
        storage.clear();
    }
}
