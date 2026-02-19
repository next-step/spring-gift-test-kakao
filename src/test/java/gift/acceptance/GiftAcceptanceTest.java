package gift.ui;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {
    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/gift-test-data.sql", executionPhase = BEFORE_TEST_METHOD)
    void 선물을_보내면_옵션_재고가_감소한다() {
        // Given: 재고 1개 옵션 준비 (SQL로 DB 직접 삽입)
        Long optionId = 1L;

        // When: 1번째 선물 보내기 성공 (재고 1 → 0)
        RestAssured
                .given()
                .log().all()
                .contentType(APPLICATION_JSON_VALUE)
                .header("Member-Id", 1L)
                .body(createGiftRequest(optionId, 1, 2L, "생일 축하"))
                .when()
                .post("/api/gifts")
                .then()
                .log().all()
                .statusCode(200);  // Spring MVC void 메서드의 기본 응답

        // Then: 재고 소진으로 2번째 선물 보내기 실패 (재고 0 → 에러)
        RestAssured
                .given()
                .log().all()
                .contentType(APPLICATION_JSON_VALUE)
                .header("Member-Id", 1L)
                .body(createGiftRequest(optionId, 1, 3L, "두번째 시도"))
                .when()
                .post("/api/gifts")
                .then()
                .log().all()
                .statusCode(500);  // IllegalStateException → 500
    }

    private String createGiftRequest(Long optionId, int quantity, Long receiverId, String message) {
        return """
            {
                "optionId": %d,
                "quantity": %d,
                "receiverId": %d,
                "message": "%s"
            }
            """.formatted(optionId, quantity, receiverId, message);
    }
}
