package gift.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * 검증 행위:
 * 5. 선물을 보내면 재고가 차감된다 (이후 재고 부족 시 실패로 증명)
 * 6. 재고보다 많은 수량을 선물하면 실패한다
 * 7. 재고를 전부 소진하면 더 이상 선물할 수 없다
 *
 * 핵심 검증 전략:
 * - 재고 차감은 DB를 직접 조회하지 않고, "다음 행동"으로 증명한다.
 * - 재고 1개짜리 옵션에 선물 1개 성공 → 이후 선물 1개 시도 → 실패
 *   → 이전 선물이 실제로 재고를 차감했음을 증명
 */
class GiftAcceptanceTest extends AcceptanceTest {

    private Long 카테고리_생성() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "전자기기"}
                        """)
                .post("/api/categories")
                .then().extract().jsonPath().getLong("id");
    }

    private Long 상품_생성(final Long categoryId) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "노트북", "price": 1500000, "imageUrl": "https://example.com/laptop.jpg", "categoryId": %d}
                        """, categoryId))
                .post("/api/products")
                .then().extract().jsonPath().getLong("id");
    }

    private Long 옵션_생성(final Long productId, final int quantity) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "블랙", "quantity": %d, "productId": %d}
                        """, quantity, productId))
                .post("/api/options")
                .then().extract().jsonPath().getLong("id");
    }

    private Long 회원_생성() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "홍길동", "email": "hong@example.com"}
                        """)
                .post("/api/members")
                .then().extract().jsonPath().getLong("id");
    }

    private void 선물_보내기(final Long senderId, final Long receiverId, final Long optionId, final int quantity) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(String.format("""
                        {"optionId": %d, "quantity": %d, "receiverId": %d, "message": "선물이에요"}
                        """, optionId, quantity, receiverId))
                .post("/api/gifts");
    }

    @Test
    void 재고가_충분하면_선물을_보낼_수_있다() {
        Long categoryId = 카테고리_생성();
        Long productId = 상품_생성(categoryId);
        Long optionId = 옵션_생성(productId, 5);
        Long senderId = 회원_생성();
        Long receiverId = 회원_생성();

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(String.format("""
                        {"optionId": %d, "quantity": 3, "receiverId": %d, "message": "선물이에요"}
                        """, optionId, receiverId))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(200);
    }

    @Test
    void 선물을_보내면_재고가_차감된다() {
        // 재고 1개짜리 옵션 준비
        Long categoryId = 카테고리_생성();
        Long productId = 상품_생성(categoryId);
        Long optionId = 옵션_생성(productId, 1);
        Long senderId = 회원_생성();
        Long receiverId = 회원_생성();

        // 첫 번째 선물: 재고 1 → 0 으로 차감 (성공)
        선물_보내기(senderId, receiverId, optionId, 1);

        // 두 번째 선물 시도: 재고가 0이므로 실패해야 한다
        // → 이것이 증명하는 것: 첫 번째 선물이 실제로 재고를 차감했다
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(String.format("""
                        {"optionId": %d, "quantity": 1, "receiverId": %d, "message": "선물이에요"}
                        """, optionId, receiverId))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);
    }

    @Test
    void 재고보다_많은_수량을_선물하면_실패한다() {
        Long categoryId = 카테고리_생성();
        Long productId = 상품_생성(categoryId);
        Long optionId = 옵션_생성(productId, 3);
        Long senderId = 회원_생성();
        Long receiverId = 회원_생성();

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(String.format("""
                        {"optionId": %d, "quantity": 5, "receiverId": %d, "message": "선물이에요"}
                        """, optionId, receiverId))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);
    }

    @Test
    void 재고를_전부_소진하면_더_이상_선물할_수_없다() {
        // 재고 2개짜리 옵션 준비
        Long categoryId = 카테고리_생성();
        Long productId = 상품_생성(categoryId);
        Long optionId = 옵션_생성(productId, 2);
        Long senderId = 회원_생성();
        Long receiverId = 회원_생성();

        // 재고 전부 소진 (2개 선물)
        선물_보내기(senderId, receiverId, optionId, 2);

        // 소진 이후 선물 시도 → 실패해야 한다
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(String.format("""
                        {"optionId": %d, "quantity": 1, "receiverId": %d, "message": "선물이에요"}
                        """, optionId, receiverId))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(500);
    }
}