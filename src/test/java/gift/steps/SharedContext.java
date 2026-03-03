package gift.steps;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class SharedContext {

    private Response response;
    private final Map<String, Object> storedIds = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void storeId(String key, Object id) {
        storedIds.put(key, id);
    }

    public Object getId(String key) {
        return storedIds.get(key);
    }
}
