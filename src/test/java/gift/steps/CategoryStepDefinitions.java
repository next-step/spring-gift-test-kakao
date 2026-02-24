package gift.steps;

import gift.application.CreateCategoryRequest;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryStepDefinitions {

	@Value("${app.port}")
	int appPort;

	private ExtractableResponse<Response> listResponse;

	@Before
	public void setUp() {
		RestAssured.port = appPort;
	}

	@Given("{string} 카테고리를 생성한다")
	public void 카테고리를_생성(String categoryName) {
		RestAssured.given().log().all()
			.contentType(ContentType.JSON)
			.body(new CreateCategoryRequest(categoryName))
			.when()
			.post("/api/categories")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();
	}

	@When("카테고리 목록을 조회하면")
	public void 카테고리_목록을_조회() {
		listResponse = RestAssured.given().log().all()
			.when()
			.get("/api/categories")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();
	}

	@Then("응답 목록에 {string} 카테고리가 포함되어 있다")
	public void 응답_목록에_생성한_카테고리가_포함되어_있다(String categoryName) {
		List<String> names = listResponse.jsonPath().getList("name", String.class);
		assertThat(names).contains(categoryName);
	}
}
