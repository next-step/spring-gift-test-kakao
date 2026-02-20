package gift;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GiftApiTest extends ApiTest {

    private Option option;
    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUpGiftData() {
        final Category category = categoryRepository.save(new Category("교환권"));
        final Product product = productRepository.save(
                new Product("스타벅스 아메리카노", 4500, "https://example.com/image.png", category)
        );
        option = optionRepository.save(new Option("기본 옵션", 100, product));
        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
    }

    @Test
    @DisplayName("선물하기 요청이 성공하면 200 응답을 반환한다")
    void give_sufficientStock_responds200() {
        // Arrange
        final String body = String.format("""
                {
                    "optionId": %d,
                    "quantity": 3,
                    "receiverId": %d,
                    "message": "생일 축하해!"
                }
                """, option.getId(), receiver.getId());

        // Act & Assert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(body)
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("재고가 부족한 선물 요청은 500 응답을 반환한다")
    void give_insufficientStock_responds500() {
        // Arrange
        final String body = String.format("""
                {
                    "optionId": %d,
                    "quantity": 101,
                    "receiverId": %d,
                    "message": "생일 축하해!"
                }
                """, option.getId(), receiver.getId());

        // Act & Assert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(body)
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }
}
