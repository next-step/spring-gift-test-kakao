package gift;

import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import gift.model.WishRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiTest {

    @LocalServerPort
    int port;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }
}
