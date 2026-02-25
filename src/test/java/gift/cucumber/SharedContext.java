package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class SharedContext {

    private Response response;
    private final Map<String, Long> memberIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void putMemberId(String name, Long id) {
        memberIds.put(name, id);
    }

    public Long getMemberId(String name) {
        return memberIds.get(name);
    }

    public void putOptionId(String name, Long id) {
        optionIds.put(name, id);
    }

    public Long getOptionId(String name) {
        return optionIds.get(name);
    }
}
