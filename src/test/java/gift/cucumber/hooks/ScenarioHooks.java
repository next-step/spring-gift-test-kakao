package gift.cucumber.hooks;

import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import gift.model.WishRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

public class ScenarioHooks {

    @Autowired
    private WishRepository wishRepository;

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
        RestAssured.port = 28080;
    }

    @After
    public void cleanUp() {
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }
}
