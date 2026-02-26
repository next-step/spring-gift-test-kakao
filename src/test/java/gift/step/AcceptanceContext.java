package gift.step;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 시나리오 내 step 간 공유 상태.
 * <p>
 * {@code @ScenarioScope}에 의해 각 시나리오마다 새로운 인스턴스가 생성되므로 시나리오 간 상태 누수가 발생하지 않는다.
 */
@Component
@ScenarioScope
public class AcceptanceContext {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;
    private final Map<String, Long> memberIds = new HashMap<>();
    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> productIds = new HashMap<>();
    private final Map<String, Long> optionIds = new HashMap<>();
    private Response response;
    private Long lastCategoryId;
    private Long lastProductId;

    // --- Response ---

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    // --- Member ---

    public void putMemberId(String name, Long id) {
        memberIds.put(name, id);
    }

    public Long getMemberId(String name) {
        return memberIds.get(name);
    }

    // --- Category ---

    public void putCategoryId(String name, Long id) {
        categoryIds.put(name, id);
        this.lastCategoryId = id;
    }

    public Long getCategoryId(String name) {
        return categoryIds.get(name);
    }

    public Long getLastCategoryId() {
        return lastCategoryId;
    }

    // --- Product ---

    public void putProductId(String name, Long id) {
        productIds.put(name, id);
        this.lastProductId = id;
    }

    public Long getProductId(String name) {
        return productIds.get(name);
    }

    public Long getLastProductId() {
        return lastProductId;
    }

    // --- Option ---

    public void putOptionId(String name, Long id) {
        optionIds.put(name, id);
    }

    public Long getOptionId(String name) {
        return optionIds.get(name);
    }

    // --- Common ---

    public Long getNotExistingId() {
        return NOT_EXISTING_ID;
    }
}
