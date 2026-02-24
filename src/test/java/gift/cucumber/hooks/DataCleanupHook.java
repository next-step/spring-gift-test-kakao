package gift.cucumber.hooks;

import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;

public class DataCleanupHook {

    private final int port;
    private final String targetBaseUrl;
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    public DataCleanupHook(
        @LocalServerPort final int port,
        @Value("${test.target.base-url:}") final String targetBaseUrl,
        final OptionRepository optionRepository,
        final ProductRepository productRepository,
        final CategoryRepository categoryRepository,
        final MemberRepository memberRepository
    ) {
        this.port = port;
        this.targetBaseUrl = targetBaseUrl;
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.memberRepository = memberRepository;
    }

    @Before
    public void setUp() {
        if (targetBaseUrl != null && !targetBaseUrl.isBlank()) {
            RestAssured.baseURI = targetBaseUrl;
            RestAssured.port = RestAssured.UNDEFINED_PORT;
        } else {
            RestAssured.baseURI = "http://localhost";
            RestAssured.port = port;
        }
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
