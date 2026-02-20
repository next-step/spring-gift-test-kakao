package gift.acceptance;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import gift.model.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    WishRepository wishRepository;

    Long senderId;
    Long receiverId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();

        senderId = memberRepository.save(new Member("보내는사람", "sender@test.com")).getId();
        receiverId = memberRepository.save(new Member("받는사람", "receiver@test.com")).getId();
    }

    @Test
    void 선물하기_성공시_재고가_요청_수량만큼_감소한다() {
        // given
        var option = 옵션_생성(10);

        // when
        var response = 선물_전달(option.getId(), 3);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(7);
    }

    @Test
    void 재고보다_많은_수량_선물시_실패하고_재고는_변경되지_않는다() {
        // given
        var option = 옵션_생성(5);

        // when
        var response = 선물_전달(option.getId(), 6);

        // then
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(5);
    }

    @Test
    void 재고와_정확히_같은_수량으로_선물하면_성공하고_재고가_0이_된다() {
        // given
        var option = 옵션_생성(5);

        // when
        var response = 선물_전달(option.getId(), 5);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(0);
    }

    @Test
    void 존재하지_않는_옵션으로_선물시_실패한다() {
        // given
        var nonExistentOptionId = 999999L;

        // when
        var response = 선물_전달(nonExistentOptionId, 1);

        // then
        assertThat(response.statusCode()).isEqualTo(500);
    }

    @Test
    void 존재하지_않는_회원이_선물시_실패한다() {
        // given
        var option = 옵션_생성(10);
        var nonExistentMemberId = 999999L;

        // when
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", nonExistentMemberId)
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(10);
    }

    @Test
    void Member_Id_헤더_없이_선물시_실패한다() {
        // given
        var option = 옵션_생성(10);

        // when
        var response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void 상품_등록부터_선물하기_전체_흐름() {
        // given
        var category = categoryRepository.save(new Category("음료"));
        var product = productRepository.save(new Product("아메리카노", 4500, "http://example.com/image.jpg", category));
        var option = optionRepository.save(new Option("ICE", 10, product));

        // when
        var response = 선물_전달(option.getId(), 2);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(8);
    }

    @Test
    void 연속_선물로_재고_소진_후_추가_선물시_실패한다() {
        // given
        var option = 옵션_생성(5);
        선물_전달(option.getId(), 3);
        선물_전달(option.getId(), 2);

        // when
        var response = 선물_전달(option.getId(), 1);

        // then
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(optionRepository.findById(option.getId()).get().getQuantity()).isEqualTo(0);
    }

    private Option 옵션_생성(int quantity) {
        var category = categoryRepository.save(new Category("음료"));
        var product = productRepository.save(new Product("아메리카노", 4500, "http://example.com/image.jpg", category));
        return optionRepository.save(new Option("ICE", quantity, product));
    }

    private ExtractableResponse<Response> 선물_전달(Long optionId, int quantity) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();
    }
}
