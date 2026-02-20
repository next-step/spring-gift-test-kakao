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
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.clear();
    }

    @Test
    void 정상_선물_보내기() {
        var category = categoryRepository.save(new Category("식품"));
        var product = productRepository.save(new Product("케이크", 30000, "https://example.com/cake.jpg", category));
        var option = optionRepository.save(new Option("기본", 10, product));
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

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
                        """.formatted(option.getId(), receiver.getId()))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);

        var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
    }

    @Test
    void 재고_부족_시_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var product = productRepository.save(new Product("케이크", 30000, "https://example.com/cake.jpg", category));
        var option = optionRepository.save(new Option("기본", 5, product));
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

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
                        """.formatted(option.getId(), receiver.getId()))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);

        var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(5);
    }

    @Test
    void 재고_경계값_두_번째_선물이_실패() {
        var category = categoryRepository.save(new Category("식품"));
        var product = productRepository.save(new Product("케이크", 30000, "https://example.com/cake.jpg", category));
        var option = optionRepository.save(new Option("기본", 1, product));
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

        var requestBody = """
                {
                    "optionId": %d,
                    "quantity": 1,
                    "receiverId": %d,
                    "message": "선물!"
                }
                """.formatted(option.getId(), receiver.getId());

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(requestBody)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);

        var afterFirst = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(afterFirst.getQuantity()).isEqualTo(0);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(requestBody)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);

        var afterSecond = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(afterSecond.getQuantity()).isEqualTo(0);
    }

    @Test
    void 존재하지_않는_옵션으로_선물_시도() {
        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

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
