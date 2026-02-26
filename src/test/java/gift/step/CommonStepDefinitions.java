package gift.step;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import java.util.List;

/**
 * 모든 Feature 에서 재사용하는 Step.
 * <p>
 * 상태 코드 검증, 단건 응답 필드 검증, 목록 응답 검증 등에 해당하는 step 이 존재한다.
 */
public class CommonStepDefinitions {

    private final AcceptanceContext context;

    public CommonStepDefinitions(AcceptanceContext context) {
        this.context = context;
    }

    // --- 상태 코드 ---

    @Then("상태 코드는 {int}이다")
    public void verifyStatusCode(int statusCode) {
        assertThat(context.getResponse().statusCode())
                .isEqualTo(statusCode);
    }

    // --- 단건 응답 필드 검증 ---

    @Then("응답의 {string}는 비어있지 않다")
    public void verifyResponseFieldNotEmpty(String field) {
        String value = context.getResponse()
                .jsonPath()
                .getString(field);

        assertThat(value).isNotBlank();
    }

    @Then("응답의 {string}은 {string}이다")
    public void verifyResponseStringField(String field, String expected) {
        String actual = context.getResponse()
                .jsonPath()
                .getString(field);

        assertThat(actual).isEqualTo(expected);
    }

    @Then("응답의 정수 {string}는 {int}이다")
    public void verifyResponseIntField(String field, int expected) {
        int actual = context.getResponse()
                .jsonPath()
                .getInt(field);

        assertThat(actual).isEqualTo(expected);
    }

    // --- 목록 응답 검증 ---

    @Then("응답 목록의 크기는 {int}이다")
    public void verifyResponseListSize(int size) {
        List<?> list = context.getResponse()
                .jsonPath()
                .getList("$");

        assertThat(list).hasSize(size);
    }

    @Then("응답 목록의 크기는 {int} 이상이다")
    public void verifyResponseListSizeAtLeast(int minSize) {
        List<?> list = context.getResponse()
                .jsonPath()
                .getList("$");

        assertThat(list.size()).isGreaterThanOrEqualTo(minSize);
    }

    @Then("응답 목록의 {string}에 {string}가 포함되어 있다")
    public void verifyResponseListContainsField(String field, String expected) {
        List<String> values = context.getResponse()
                .jsonPath()
                .getList(field, String.class);

        assertThat(values).contains(expected);
    }
}
