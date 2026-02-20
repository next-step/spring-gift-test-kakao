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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
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

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("유효한 선물 요청 시 성공한다")
    void giveGift() {
        Long senderId = memberRepository.save(new Member("보내는사람", "sender@test.com")).getId();
        Long receiverId = memberRepository.save(new Member("받는사람", "receiver@test.com")).getId();
        Long optionId = 옵션을_셋업한다("옵션A", 10);

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                    {"optionId": %d, "quantity": 1, "receiverId": %d, "message": "생일 축하해"}
                    """.formatted(optionId, receiverId))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("재고 전량 선물 후 추가 선물 시 실패한다")
    void giveGiftExceedingStock() {
        Long senderId = memberRepository.save(new Member("보내는사람", "sender@test.com")).getId();
        Long receiverId = memberRepository.save(new Member("받는사람", "receiver@test.com")).getId();
        Long optionId = 옵션을_셋업한다("옵션A", 10);

        선물을_보낸다(senderId, optionId, 10, receiverId, "첫 번째 선물");

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                    {"optionId": %d, "quantity": 1, "receiverId": %d, "message": "두 번째 선물"}
                    """.formatted(optionId, receiverId))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 선물 시 실패한다")
    void giveGiftWithInvalidOption() {
        Long senderId = memberRepository.save(new Member("보내는사람", "sender@test.com")).getId();
        Long receiverId = memberRepository.save(new Member("받는사람", "receiver@test.com")).getId();

        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                    {"optionId": 9999, "quantity": 1, "receiverId": %d, "message": "선물"}
                    """.formatted(receiverId))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }

    private Long 옵션을_셋업한다(String name, int quantity) {
        Category category = categoryRepository.save(new Category("교환권"));
        Product product = productRepository.save(new Product("아메리카노", 5000, "http://img.com/a.jpg", category));
        return optionRepository.save(new Option(name, quantity, product)).getId();
    }

    private void 선물을_보낸다(Long senderId, Long optionId, int quantity, Long receiverId, String message) {
        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body("""
                    {"optionId": %d, "quantity": %d, "receiverId": %d, "message": "%s"}
                    """.formatted(optionId, quantity, receiverId, message))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200);
    }
}
