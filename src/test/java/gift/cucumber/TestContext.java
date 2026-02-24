package gift.cucumber;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class TestContext {

    private Response lastResponse;
    private Category lastCategory;
    private Product lastProduct;
    private Option lastOption;
    private final Map<String, Member> members = new HashMap<>();

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public Category getLastCategory() {
        return lastCategory;
    }

    public void setLastCategory(Category lastCategory) {
        this.lastCategory = lastCategory;
    }

    public Product getLastProduct() {
        return lastProduct;
    }

    public void setLastProduct(Product lastProduct) {
        this.lastProduct = lastProduct;
    }

    public Option getLastOption() {
        return lastOption;
    }

    public void setLastOption(Option lastOption) {
        this.lastOption = lastOption;
    }

    public void addMember(String name, Member member) {
        members.put(name, member);
    }

    public Member getMember(String name) {
        return members.get(name);
    }
}
