package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * G1: 재고가 충분할 때 선물하기에 성공한다.
     * - 옵션1(재고 10) 에 수량 1을 선물 → 200 응답
     */
    @Test
    void 재고가_충분할_때_선물하기에_성공한다() {
        // given
        long senderId = 1L;
        long receiverId = 2L;
        long optionId = 1L;

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "생일 축하해!"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }

    /**
     * G2: 선물을 보내면 재고가 감소한다.
     * - 옵션1(재고 10)을 10개 전부 선물 → 성공
     * - 같은 옵션에 1개 추가 선물 → 재고 부족으로 실패
     * - 두 번째 실패가 곧 "재고가 감소했다"는 증거
     */
    @Test
    void 선물을_보내면_재고가_감소한다() {
        // given
        long senderId = 1L;
        long receiverId = 2L;
        long optionId = 1L;

        // when — 재고 10개 전부 소진
        ExtractableResponse<Response> firstResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", 10,
                        "receiverId", receiverId,
                        "message", "전부 보낸다"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then — 첫 번째 요청 성공
        assertThat(firstResponse.statusCode()).isEqualTo(200);

        // when — 같은 옵션에 1개 추가 요청
        ExtractableResponse<Response> secondResponse = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "하나 더"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then — 두 번째 요청 실패 (재고 부족)
        assertThat(secondResponse.statusCode()).isEqualTo(500);
    }

    /**
     * G3: 재고보다 많은 수량을 선물하면 실패한다.
     * - 옵션2(재고 1)에 수량 2를 요청 → 500 응답
     */
    @Test
    void 재고보다_많은_수량을_선물하면_실패한다() {
        // given
        long senderId = 1L;
        long receiverId = 2L;
        long optionId = 2L; // 재고 1개

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", 2,
                        "receiverId", receiverId,
                        "message", "재고 초과 테스트"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(500);
    }

    /**
     * G4: 존재하지 않는 옵션으로 선물하면 실패한다.
     * - 옵션 ID 9999 (존재하지 않음) → 500 응답
     */
    @Test
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        // given
        long senderId = 1L;
        long receiverId = 2L;
        long nonExistentOptionId = 9999L;

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", nonExistentOptionId,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "없는 옵션 테스트"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(500);
    }

    /**
     * G5: Member-Id 헤더 없이 선물하면 실패한다.
     * - Member-Id 헤더 누락 → 400 응답
     */
    @Test
    void Member_Id_헤더_없이_선물하면_실패한다() {
        // given
        long receiverId = 2L;
        long optionId = 1L;

        // when — Member-Id 헤더 없이 요청
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "헤더 누락 테스트"
                ))
                .when().post("/api/gifts")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(400);
    }
}
