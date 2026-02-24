package gift.acceptance;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import gift.support.ScenarioContext;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;

public class CommonStepDefinitions {

	private final ScenarioContext context;

	public CommonStepDefinitions(ScenarioContext context) {
		this.context = context;
	}

	@Then("요청이 성공한다")
	public void 요청이_성공한다() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(200);
	}

	@Then("요청이 실패한다")
	public void 요청이_실패한다() {
		Response response = context.get("response", Response.class);
		response.then().statusCode(500);
	}

	@Then("응답에 id가 포함되어 있다")
	public void 응답에_id가_포함되어_있다() {
		Response response = context.get("response", Response.class);
		response.then().body("id", notNullValue());
	}

	@Then("응답 목록의 크기가 {int}이다")
	public void 응답_목록의_크기가_N이다(int size) {
		Response response = context.get("response", Response.class);
		response.then().body("size()", equalTo(size));
	}

	@Then("응답의 {string} 값이 {string}이다")
	public void 응답_문자열_값_검증(String field, String expected) {
		Response response = context.get("response", Response.class);
		response.then().body(field, equalTo(expected));
	}

	@Then("응답의 {string} 값이 {int}이다")
	public void 응답_정수_값_검증(String field, int expected) {
		Response response = context.get("response", Response.class);
		response.then().body(field, equalTo(expected));
	}
}
