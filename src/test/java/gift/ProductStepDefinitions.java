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
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ProductStepDefinitions {

	private static final String BASE_URL = "http://localhost:28080/api";

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private SharedContext sharedContext;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@When("{string}를 가격 {int}원과 이미지 {string}로 생성한다.")
	public void 상품을_생성한다(String name, int price, String imageUrl) {
		Category category = categoryRepository.findAll().get(0);
		createProduct(name, price, imageUrl, category.getId());
	}

	@Given("{string}가 가격 {int}원과 이미지 {string}로 등록되어있다.")
	public void 상품이_등록되어있다(String name, int price, String imageUrl) {
		Category category = categoryRepository.findAll().get(0);
		productRepository.save(new Product(name, price, imageUrl, category));
	}

	@When("상품 목록을 조회한다.")
	public void 상품_목록을_조회한다() {
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(
				BASE_URL + "/products",
				String.class
			);
			sharedContext.setStatusCode(response.getStatusCode().value());
			sharedContext.setResponseBody(response.getBody());
		} catch (HttpStatusCodeException e) {
			sharedContext.setStatusCode(e.getStatusCode().value());
			sharedContext.setResponseBody(e.getResponseBodyAsString());
		}
	}

	@When("존재하지 않는 카테고리의 {string} 상품을 가격 {int}원, 이미지 {string}으로 생성한다.")
	public void 존재하지_않는_카테고리로_상품을_생성한다(String name, int price, String imageUrl) {
		createProduct(name, price, imageUrl, 99999L);
	}

	@Then("상품의 이름은 {string} 이다.")
	public void 상품의_이름은_이다(String expectedName) throws Exception {
		JsonNode json = objectMapper.readTree(sharedContext.getResponseBody());
		assertThat(json.get("name").asText()).isEqualTo(expectedName);
	}

	@Then("상품의 가격은 {int}원 이다.")
	public void 상품의_가격은_이다(int expectedPrice) throws Exception {
		JsonNode json = objectMapper.readTree(sharedContext.getResponseBody());
		assertThat(json.get("price").asInt()).isEqualTo(expectedPrice);
	}

	@Then("선물 목록의 크기는 {int}이다.")
	public void 선물_목록의_크기는_N이다(int expectedSize) throws Exception {
		JsonNode json = objectMapper.readTree(sharedContext.getResponseBody());
		assertThat(json.size()).isEqualTo(expectedSize);
	}

	private void createProduct(String name, int price, String imageUrl, Long categoryId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> request = Map.of(
			"name", name,
			"price", price,
			"imageUrl", imageUrl,
			"categoryId", categoryId
		);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(
				BASE_URL + "/products",
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
}
