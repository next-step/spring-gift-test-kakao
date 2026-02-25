package gift.restassured;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static gift.Fixtures.member;
import static gift.Fixtures.option;
import static gift.Fixtures.product;
import static org.assertj.core.api.Assertions.assertThat;

class GiftAcceptanceTest extends AcceptanceTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void 정상_선물_보내기() {
        var category = categoryRepository.save(new Category("식품"));
        var saved = productRepository.save(product("케이크", category));
        var opt = optionRepository.save(option(10, saved));
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        선물을_보낸다(sender.getId(), opt.getId(), 3, receiver.getId())
                .then().statusCode(200);

        assertThat(옵션_재고를_조회한다(opt.getId())).isEqualTo(7);
    }

    @Test
    void 재고_부족_시_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var saved = productRepository.save(product("케이크", category));
        var opt = optionRepository.save(option(5, saved));
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        선물을_보낸다(sender.getId(), opt.getId(), 10, receiver.getId())
                .then().statusCode(500);

        assertThat(옵션_재고를_조회한다(opt.getId())).isEqualTo(5);
    }

    @Test
    void 재고_경계값_두_번째_선물이_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var saved = productRepository.save(product("케이크", category));
        var opt = optionRepository.save(option(1, saved));
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        선물을_보낸다(sender.getId(), opt.getId(), 1, receiver.getId())
                .then().statusCode(200);
        assertThat(옵션_재고를_조회한다(opt.getId())).isEqualTo(0);

        선물을_보낸다(sender.getId(), opt.getId(), 1, receiver.getId())
                .then().statusCode(500);
        assertThat(옵션_재고를_조회한다(opt.getId())).isEqualTo(0);
    }

    @Test
    void 존재하지_않는_옵션으로_선물_시도() {
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        선물을_보낸다(sender.getId(), 999999L, 1, receiver.getId())
                .then().statusCode(500);
    }

    private Response 선물을_보낸다(Long senderId, Long optionId, int quantity, Long receiverId) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(optionId, quantity, receiverId))
                .when()
                .post("/api/gifts");
    }

    private int 옵션_재고를_조회한다(Long optionId) {
        return optionRepository.findById(optionId).orElseThrow().getQuantity();
    }
}
