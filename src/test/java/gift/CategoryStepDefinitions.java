package gift;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CategoryStepDefinitions {

	private static final String BASE_URL = "http://localhost:28080/api";

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private SharedContext sharedContext;

	@Autowired
	private CategoryRepository categoryRepository;

	@Given("{string} 카테고리가 등록되어 있다.")
	public void 카테고리가_등록되어_있다(String name) {
		categoryRepository.save(new Category(name));
	}

	@When("{string} 카테고리를 생성한다.")
	public void 카테고리를_생성한다(String name) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> request = Map.of("name", name);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(
				BASE_URL + "/categories",
				new HttpEntity<>(request, headers),
				String.class
			);
			sharedContext.setStatusCode(response.getStatusCode().value());
			sharedContext.setResponseBody(response.getBody());
		} catch (HttpStatusCodeException e) {
			sharedContext.setStatusCode(e.getStatusCode().value());
			sharedContext.setResponseBody(e.getResponseBodyAsString());
		}
	}

	@When("카테고리 목록을 조회한다.")
	public void 카테고리_목록을_조회한다() {
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(
				BASE_URL + "/categories",
				String.class
			);
			sharedContext.setStatusCode(response.getStatusCode().value());
			sharedContext.setResponseBody(response.getBody());
		} catch (HttpStatusCodeException e) {
			sharedContext.setStatusCode(e.getStatusCode().value());
			sharedContext.setResponseBody(e.getResponseBodyAsString());
		}
	}

	@Then("응답의 카테고리 이름은 {string}이다.")
	public void 응답의_카테고리_이름은_이다(String expectedName) throws Exception {
		JsonNode json = objectMapper.readTree(sharedContext.getResponseBody());
		assertThat(json.get("name").asText()).isEqualTo(expectedName);
	}

	@Then("카테고리 목록의 크기는 {int}이다.")
	public void 카테고리_목록의_크기는_N이다(int expectedSize) throws Exception {
		JsonNode json = objectMapper.readTree(sharedContext.getResponseBody());
		assertThat(json.size()).isEqualTo(expectedSize);
	}
}
