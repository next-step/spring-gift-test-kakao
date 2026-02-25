package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private ExtractableResponse<Response> response;

    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();
    private final Map<String, Long> memberIds = new HashMap<>();

    private long optionSeq = 1;
    private long memberSeq = 1;

    public ExtractableResponse<Response> getResponse() {
        return response;
    }

    public void setResponse(ExtractableResponse<Response> response) {
        this.response = response;
    }

    public void putCategoryId(String name, long id) {
        categoryIds.put(name, id);
    }

    public long getCategoryId(String name) {
        return categoryIds.get(name);
    }

    public void putProductId(String name, long id) {
        productIds.put(name, id);
    }

    public long getProductId(String name) {
        return productIds.get(name);
    }

    public long nextOptionId() {
        return optionSeq++;
    }

    public void putOptionId(String name, long id) {
        optionIds.put(name, id);
    }

    public long getOptionId(String name) {
        return optionIds.get(name);
    }

    public long nextMemberId() {
        return memberSeq++;
    }

    public void putMemberId(String name, long id) {
        memberIds.put(name, id);
    }

    public long getMemberId(String name) {
        return memberIds.get(name);
    }
}
