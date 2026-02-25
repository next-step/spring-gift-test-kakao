package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {
    private int responseStatusCode;
    private final Map<String, Long> ids = new HashMap<>();

    public int getResponseStatusCode() {
        return responseStatusCode;
    }

    public void setResponseStatusCode(int responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
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
