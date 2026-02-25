package gift.acceptance;

import gift.support.DatabaseCleanup;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class CommonStepDefinitions {

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private AcceptanceContext context;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        databaseCleanup.execute();
    }

    @그러면("카테고리가 정상적으로 생성된다")
    public void 카테고리가_정상적으로_생성된다() {
        assertThat(context.getResponse().statusCode(), equalTo(200));
        assertThat(context.getResponse().jsonPath().get("id"), notNullValue());
    }

    @그러면("상품이 정상적으로 생성된다")
    public void 상품이_정상적으로_생성된다() {
        assertThat(context.getResponse().statusCode(), equalTo(200));
        assertThat(context.getResponse().jsonPath().get("id"), notNullValue());
    }

    @그러면("정상적으로 조회된다")
    public void 정상적으로_조회된다() {
        assertThat(context.getResponse().statusCode(), equalTo(200));
    }

    @그러면("선물이 정상적으로 전달된다")
    public void 선물이_정상적으로_전달된다() {
        assertThat(context.getResponse().statusCode(), equalTo(200));
    }

    @그러면("선물 전달이 실패한다")
    public void 선물_전달이_실패한다() {
        assertThat(context.getResponse().statusCode(), equalTo(500));
    }

    @그러면("상품 생성이 실패한다")
    public void 상품_생성이_실패한다() {
        assertThat(context.getResponse().statusCode(), equalTo(500));
    }

    @그리고("응답의 {string} 필드는 {string}이다")
    public void 응답의_필드는_이다(String fieldName, String expectedValue) {
        assertThat(context.getResponse().jsonPath().getString(fieldName), equalTo(expectedValue));
    }

    @그리고("응답 목록의 {string} 필드에 {string}이 포함되어 있다")
    public void 응답_목록의_필드에_이_포함되어_있다(String fieldName, String expectedValue) {
        assertThat(context.getResponse().jsonPath().getList(fieldName, String.class), hasItem(expectedValue));
    }

    @그리고("응답의 {string} 필드는 정수 {int}이다")
    public void 응답의_필드는_정수_이다(String fieldName, int expectedValue) {
        assertThat(context.getResponse().jsonPath().getInt(fieldName), equalTo(expectedValue));
    }

    @그리고("응답은 빈 목록이다")
    public void 응답은_빈_목록이다() {
        assertThat(context.getResponse().jsonPath().getList("$"), empty());
    }
}