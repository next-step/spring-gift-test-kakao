package gift.cucumber.steps;

import gift.cucumber.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class CommonSteps {

    @Autowired
    private TestContext testContext;

    @그러면("응답 코드는 {int}이다")
    public void 응답_코드는_이다(int statusCode) {
        testContext.getLastResponse().then().statusCode(statusCode);
    }

    @그리고("응답에 id가 포함되어 있다")
    public void 응답에_id가_포함되어_있다() {
        testContext.getLastResponse().then().body("id", notNullValue());
    }

    @그리고("목록에 {int}개의 항목이 있다")
    public void 목록에_n개의_항목이_있다(int count) {
        testContext.getLastResponse().then().body("$", hasSize(count));
    }
}
