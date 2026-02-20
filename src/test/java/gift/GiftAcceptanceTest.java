package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

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

    private Member sender;
    private Member receiver;
    private Option option;

    @BeforeEach
    void setUp() {
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Category category = categoryRepository.save(new Category("식품"));
        Product product = productRepository.save(new Product("아메리카노", 4500, "http://example.com/image.png", category));
        option = optionRepository.save(new Option("ICE", 10, product));
    }

    @DisplayName("선물을 보낸다")
    @Test
    void 선물을_보낸다() {
        var response = giveGift(option.getId(), 3, receiver.getId(), "생일 축하해!", sender.getId());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }

    @DisplayName("존재하지 않는 옵션으로 선물하면 실패한다")
    @Test
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        var response = giveGift(999L, 1, receiver.getId(), "선물", sender.getId());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @DisplayName("재고보다 많은 수량을 선물하면 실패한다")
    @Test
    void 재고보다_많은_수량을_선물하면_실패한다() {
        var response = giveGift(option.getId(), 999, receiver.getId(), "선물", sender.getId());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    ExtractableResponse<Response> giveGift(Long optionId, int quantity, Long receiverId, String message, Long memberId) {
        return RestAssured.given().log().all()
                .port(port)
                .contentType("application/json")
                .header("Member-Id", memberId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", message
                ))
                .when()
                .post("/api/gifts")
                .then().log().all()
                .extract();
    }
}
