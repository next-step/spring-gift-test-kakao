package gift;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

class GiftAcceptanceTest extends AcceptanceTest {

    // gift SQL 스크립트에 정의된 테스트 픽스처 ID
    private static final long SENDER_ID = 1L;
    private static final long RECEIVER_ID = 2L;
    private static final long TALL_OPTION_ID = 1L;
    private static final int INITIAL_STOCK = 10;
    private static final long NON_EXISTENT_OPTION_ID = 999L;

    @Test
    @Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql", "/sql/option-data.sql", "/sql/member-data.sql"}, executionPhase = BEFORE_TEST_METHOD)
    void 선물을_보내면_재고가_차감된다() {
        // When: 재고 10개 중 7개를 선물
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 7, RECEIVER_ID).then().statusCode(200);

        // Then: 나머지 5개를 선물하면 실패해야 한다 (잔여 3개 < 요청 5개)
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 5, RECEIVER_ID).then()
                .statusCode(500); // TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경
    }

    @Test
    @Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql", "/sql/option-data.sql", "/sql/member-data.sql"}, executionPhase = BEFORE_TEST_METHOD)
    void 재고보다_많은_수량으로_선물을_보내면_실패한다() {
        // When & Then: 재고 10개인데 11개를 선물
        선물_보내기(SENDER_ID, TALL_OPTION_ID, INITIAL_STOCK + 1, RECEIVER_ID).then()
                .statusCode(500); // TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경
    }

    @Test
    @Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql", "/sql/option-data.sql", "/sql/member-data.sql"}, executionPhase = BEFORE_TEST_METHOD)
    void 존재하지_않는_옵션으로_선물을_보내면_실패한다() {
        // When & Then
        선물_보내기(SENDER_ID, NON_EXISTENT_OPTION_ID, 1, RECEIVER_ID).then()
                .statusCode(500); // TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경
    }

    @Test
    @Sql(scripts = {"/sql/category-data.sql", "/sql/product-data.sql", "/sql/option-data.sql", "/sql/member-data.sql"}, executionPhase = BEFORE_TEST_METHOD)
    void 선물을_보낸_후_동일_옵션으로_다시_보내면_누적_차감된다() {
        // When: 3개 선물 후 4개 추가 선물 (총 7개 차감)
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 3, RECEIVER_ID).then().statusCode(200);
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 4, RECEIVER_ID).then().statusCode(200);

        // Then: 잔여 3개이므로 4개 요청은 실패해야 한다
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 4, RECEIVER_ID).then()
                .statusCode(500); // TODO: 에러 핸들링 구현 후 적절한 상태 코드로 변경

        // 잔여 3개이므로 3개 요청은 성공해야 한다
        선물_보내기(SENDER_ID, TALL_OPTION_ID, 3, RECEIVER_ID).then().statusCode(200);
    }

    private Response 선물_보내기(long memberId, long optionId, int quantity, long receiverId) {
        String body = """
                {
                    "optionId": %d,
                    "quantity": %d,
                    "receiverId": %d,
                    "message": "테스트 선물"
                }
                """.formatted(optionId, quantity, receiverId);

        return given()
                .header("Member-Id", memberId)
                .contentType("application/json")
                .body(body)
        .when()
                .post("/api/gifts");
    }
}
