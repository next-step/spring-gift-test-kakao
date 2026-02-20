package gift.acceptance.gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import gift.support.CategoryFixture;
import gift.support.DatabaseCleanup;
import gift.support.MemberFixture;
import gift.support.OptionFixture;
import gift.support.ProductFixture;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleanup databaseCleanup;

    @Autowired
    CategoryFixture categoryFixture;

    @Autowired
    ProductFixture productFixture;

    @Autowired
    OptionFixture optionFixture;

    @Autowired
    MemberFixture memberFixture;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }

    @Test
    void 선물을_전달한다() {
        Category category = categoryFixture.builder().name("음료").build();
        Product product = productFixture.builder().name("아메리카노").price(4500).category(category).build();
        Option option = optionFixture.builder().name("ICE").quantity(3).product(product).build();
        Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();
        Member receiver = memberFixture.builder().name("받는사람").email("receiver@test.com").build();

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 3,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "한번더"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }

    @Test
    void 재고보다_많은_수량을_요청하면_실패한다() {
        Category category = categoryFixture.builder().name("음료").build();
        Product product = productFixture.builder().name("아메리카노").price(4500).category(category).build();
        Option option = optionFixture.builder().name("ICE").quantity(1).product(product).build();
        Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();
        Member receiver = memberFixture.builder().name("받는사람").email("receiver@test.com").build();

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 100,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }

    @Test
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        Member sender = memberFixture.builder().name("보내는사람").email("sender@test.com").build();

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", 999999,
                        "quantity", 1,
                        "receiverId", 999999,
                        "message", "선물입니다"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }
}
