package gift.steps;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioState {

    private Response response;
    private final Map<String, Object> entities = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(final Response response) {
        this.response = response;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String key) {
        return (T) entities.get(key);
    }

    public void put(final String key, final Object value) {
        entities.put(key, value);
    }
}
