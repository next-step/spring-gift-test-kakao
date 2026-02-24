package gift.acceptance.gift;

import gift.model.Member;
import gift.model.Option;
import gift.support.DatabaseCleanup;
import gift.support.MemberFixture;
import gift.support.OptionFixture;
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
    OptionFixture optionFixture;

    @Autowired
    MemberFixture memberFixture;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }

    Long createCategoryAndReturnId(String name) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .extract()
                .jsonPath()
                .getLong("id");
    }

    Long createProductAndReturnId(String name, int price, Long categoryId) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", "https://example.com/image.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @Test
    void 선물을_전달한다() {
        Long categoryId = createCategoryAndReturnId("음료");
        Long productId = createProductAndReturnId("아메리카노", 4500, categoryId);
        Option option = optionFixture.builder().name("ICE").quantity(3).productId(productId).build();
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
    }

    @Test
    void 재고보다_많은_수량을_요청하면_실패한다() {
        Long categoryId = createCategoryAndReturnId("음료");
        Long productId = createProductAndReturnId("아메리카노", 4500, categoryId);
        Option option = optionFixture.builder().name("ICE").quantity(1).productId(productId).build();
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