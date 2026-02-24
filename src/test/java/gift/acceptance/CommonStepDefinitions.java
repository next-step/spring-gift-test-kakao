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

    @그러면("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는_이다(int statusCode) {
        assertThat(context.getResponse().statusCode(), equalTo(statusCode));
    }

    @그리고("응답의 {string} 필드는 비어있지 않다")
    public void 응답의_필드는_비어있지_않다(String fieldName) {
        assertThat(context.getResponse().jsonPath().get(fieldName), notNullValue());
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