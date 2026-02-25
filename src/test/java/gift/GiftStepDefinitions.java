package gift;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class GiftStepDefinitions {
	private static final String BASE_URL = "http://localhost:28080/api";

	private final RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private SharedContext sharedContext;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OptionRepository optionRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member sender;
	private Member receiver;
	private Option option;
	private Long invalidOptionId;
	private Long invalidSenderId;

	// === Background Steps ===

	@Given("선물을 보내는 사람이 존재하고, 이름과 이메일로 구성되어있다.")
	public void 보내는_사람이_존재한다() {
		sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
	}

	@Given("선물을 받는 사람이 존재하고, 이름과 이메일로 구성되어있다.")
	public void 받는_사람이_존재한다() {
		receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
	}

	// === Given Steps ===

	@Given("선물의 재고가 {int}개 있다.")
	public void 선물의_재고가_N개_있다(int stock) {
		Category category = categoryRepository.save(new Category("테스트 카테고리"));
		Product product = productRepository.save(
			new Product("테스트 상품", 10000, "http://test.jpg", category)
		);
		option = optionRepository.save(new Option("테스트 옵션", stock, product));
	}

	@Given("선물의 옵션ID를 존재하지 않는 {int}로 선택한다.")
	public void 존재하지_않는_옵션ID를_선택한다(int optionId) {
		invalidOptionId = (long)optionId;
	}

	@Given("존재하지 않는 발신자 Member ID {int}가 있다.")
	public void 존재하지_않는_발신자_Member_ID가_있다(int memberId) {
		invalidSenderId = (long)memberId;
	}

	// === When Steps ===

	@When("선물할 개수를 {int}개로 선택한다.")
	public void 선물할_개수를_N개로_선택한다(int quantity) {
		Long optionId = (invalidOptionId != null) ? invalidOptionId : option.getId();
		sendGift(optionId, quantity, sender.getId());
	}

	@When("존재하지 않는 발신자가 선물할 개수를 {int}개로 선택한다.")
	public void 존재하지_않는_발신자가_선물할_개수를_N개로_선택한다(int quantity) {
		sendGift(option.getId(), quantity, invalidSenderId);
	}

	// === Then Steps ===

	@Then("선물의 재고는 {int}개가 된다.")
	public void 선물의_재고는_N개가_된다(int expectedStock) {
		int actualStock = optionRepository.findById(option.getId())
			.orElseThrow()
			.getQuantity();
		assertThat(actualStock).isEqualTo(expectedStock);
	}

	// === Helper ===

	private void sendGift(Long optionId, int quantity, Long senderId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Member-Id", String.valueOf(senderId));

		Map<String, Object> request = Map.of(
			"optionId", optionId,
			"quantity", quantity,
			"receiverId", receiver.getId(),
			"message", "선물입니다"
		);

		try {
			ResponseEntity<Void> response = restTemplate.exchange(
				BASE_URL + "/gifts",
				HttpMethod.POST,
				new HttpEntity<>(request, headers),
				Void.class
			);
			sharedContext.setStatusCode(response.getStatusCode().value());
		} catch (HttpStatusCodeException e) {
			sharedContext.setStatusCode(e.getStatusCode().value());
		}
	}
}
