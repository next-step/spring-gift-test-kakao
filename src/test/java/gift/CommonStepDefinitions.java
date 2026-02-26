package gift;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

public class CommonStepDefinitions {

	@Autowired
	private SharedContext sharedContext;

	@Autowired
	private OptionRepository optionRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Before
	public void setUp() {
		optionRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Then("응답 상태 코드는 {int}을 반환한다.")
	public void 응답_상태_코드는_N을_반환한다(int statusCode) {
		assertThat(sharedContext.getStatusCode()).isEqualTo(statusCode);
	}
}
