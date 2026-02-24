package gift.cucumber.steps;

import gift.cucumber.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;

import static org.hamcrest.Matchers.equalTo;

public class CommonSteps {

    private final TestContext context;

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @그러면("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드_검증(int statusCode) {
        context.getResponse().then().statusCode(statusCode);
    }

    @그리고("응답 본문의 {string}은 {string}이다")
    public void 응답_본문_문자열_검증(String field, String value) {
        context.getResponse().then().body(field, equalTo(value));
    }

    @그리고("응답 본문의 {string}은 정수 {int}이다")
    public void 응답_본문_정수_검증(String field, int value) {
        context.getResponse().then().body(field, equalTo(value));
    }

    @그리고("응답 목록의 크기는 {int}이다")
    public void 응답_목록_크기_검증(int size) {
        context.getResponse().then().body("size()", equalTo(size));
    }

    @그리고("응답 목록에 {string}가 포함되어 있다")
    public void 응답_목록에_이름이_포함(String name) {
        context.getResponse().then().body("name", org.hamcrest.Matchers.hasItem(name));
    }
}
