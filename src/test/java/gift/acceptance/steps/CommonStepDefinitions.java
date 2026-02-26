package gift.acceptance.steps;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class CommonStepDefinitions {

    static Response latestResponse;
    static int latestStatusCode;
    static Map<String, Long> memberIds = new HashMap<>();
    static Map<String, Long> categoryIds = new HashMap<>();
    static Map<String, Long> optionIds = new HashMap<>();
    static Long defaultProductId;

    public static void reset() {
        latestResponse = null;
        latestStatusCode = 0;
        memberIds.clear();
        categoryIds.clear();
        optionIds.clear();
        defaultProductId = null;
    }

    @Then("응답 상태코드가 {int}이다")
    public void 응답_상태코드가(int statusCode) {
        assertThat(latestStatusCode).isEqualTo(statusCode);
    }

    @Then("응답 상태코드가 {int}이 아니다")
    public void 응답_상태코드가_아니다(int statusCode) {
        assertThat(latestStatusCode).isNotEqualTo(statusCode);
    }

    @Then("{string} 항목이 목록에 존재한다")
    public void 항목이_목록에_존재한다(String name) {
        latestResponse.then()
            .body("name", hasItem(name));
    }
}
