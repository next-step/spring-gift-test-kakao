package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.MemberRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body("""
                        {
                            "optionId": %d,
                            "quantity": 3,
                            "receiverId": %d,
                            "message": "생일 축하해!"
                        }
                        """.formatted(opt.getId(), receiver.getId()))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);

        var updatedOption = optionRepository.findById(opt.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
    }

    @Test
    void 재고_부족_시_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var saved = productRepository.save(product("케이크", category));
        var opt = optionRepository.save(option(5, saved));
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body("""
                        {
                            "optionId": %d,
                            "quantity": 10,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(opt.getId(), receiver.getId()))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);

        var updatedOption = optionRepository.findById(opt.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(5);
    }

    @Test
    void 재고_경계값_두_번째_선물이_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var saved = productRepository.save(product("케이크", category));
        var opt = optionRepository.save(option(1, saved));
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        var requestBody = """
                {
                    "optionId": %d,
                    "quantity": 1,
                    "receiverId": %d,
                    "message": "선물!"
                }
                """.formatted(opt.getId(), receiver.getId());

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(requestBody)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);

        var afterFirst = optionRepository.findById(opt.getId()).orElseThrow();
        assertThat(afterFirst.getQuantity()).isEqualTo(0);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(requestBody)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);

        var afterSecond = optionRepository.findById(opt.getId()).orElseThrow();
        assertThat(afterSecond.getQuantity()).isEqualTo(0);
    }

    @Test
    void 존재하지_않는_옵션으로_선물_시도() {
        var sender = memberRepository.save(member("보내는사람"));
        var receiver = memberRepository.save(member("받는사람"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body("""
                        {
                            "optionId": 999999,
                            "quantity": 1,
                            "receiverId": %d,
                            "message": "선물!"
                        }
                        """.formatted(receiver.getId()))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);
    }
}
