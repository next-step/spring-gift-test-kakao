package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

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
    @DisplayName("유효한 요청으로 선물하기를 하면 성공한다")
    @Sql(scripts = "/data/gift-acceptance/선물하기_성공.sql", executionPhase = BEFORE_TEST_METHOD)
    void 선물하기_성공() {
        // Given: 재고가 10개인 옵션과 보내는이/받는이가 존재하는 상태
        var request = Map.of(
                "optionId", 1,
                "quantity", 1,
                "receiverId", 2,
                "message", "생일 축하해!"
        );

        // When: 선물하기 API 호출
        // Then: 성공 (200 OK)
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 1L)
                    .body(request)
                .when()
                    .post("/api/gifts")
                .then().log().all()
                    .statusCode(200);
    }

    @Test
    @DisplayName("선물하기 성공 후 재고가 차감되어, 재고 부족 시 동일 요청이 실패한다")
    @Sql(scripts = "/data/gift-acceptance/선물하기_재고차감_검증.sql", executionPhase = BEFORE_TEST_METHOD)
    void 선물하기_성공_후_재고가_차감되어_동일_요청이_실패한다() {
        // Given: 재고가 1개인 옵션이 존재하는 상태
        var request = Map.of(
                "optionId", 1,
                "quantity", 1,
                "receiverId", 2,
                "message", "생일 축하해!"
        );

        // When: 첫 번째 선물하기 (재고 1 → 0)
        // Then: 성공
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 1L)
                    .body(request)
                .when()
                    .post("/api/gifts")
                .then().log().all()
                    .statusCode(200);

        // When: 동일한 옵션으로 두 번째 선물하기 시도
        // Then: 재고 부족으로 실패 (상태 변화 증명)
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 1L)
                    .body(request)
                .when()
                    .post("/api/gifts")
                .then().log().all()
                    .statusCode(500);
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 선물하기를 하면 실패한다")
    @Sql(scripts = "/data/gift-acceptance/존재하지_않는_옵션으로_선물하기_실패.sql", executionPhase = BEFORE_TEST_METHOD)
    void 존재하지_않는_옵션으로_선물하기_실패() {
        // Given: DB에 옵션이 존재하지 않는 상태
        var request = Map.of(
                "optionId", 999,
                "quantity", 1,
                "receiverId", 2,
                "message", "생일 축하해!"
        );

        // When: 존재하지 않는 옵션 ID로 선물하기 시도
        // Then: 실패
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 1L)
                    .body(request)
                .when()
                    .post("/api/gifts")
                .then().log().all()
                    .statusCode(500);
    }

    @Test
    @DisplayName("존재하지 않는 보내는 사람으로 선물하기를 하면 실패한다")
    @Sql(scripts = "/data/gift-acceptance/존재하지_않는_보내는_사람으로_선물하기_실패.sql", executionPhase = BEFORE_TEST_METHOD)
    void 존재하지_않는_보내는_사람으로_선물하기_실패() {
        // Given: 옵션은 존재하지만, Member-Id에 해당하는 회원이 DB에 없는 상태
        var request = Map.of(
                "optionId", 1,
                "quantity", 1,
                "receiverId", 2,
                "message", "생일 축하해!"
        );

        // When: 존재하지 않는 회원 ID(999)로 선물하기 시도
        // Then: 배송 단계에서 보내는 사람 조회 실패
        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .header("Member-Id", 999L)
                    .body(request)
                .when()
                    .post("/api/gifts")
                .then().log().all()
                    .statusCode(500);
    }
}
