package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {
    private Response response;
    private final Map<String, Long> ids = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void storeId(String name, long id) {
        ids.put(name, id);
    }

    public long getId(String name) {
        Long id = ids.get(name);
        if (id == null) {
            throw new IllegalStateException("ID not found for: " + name);
        }
        return id;
    }
}
