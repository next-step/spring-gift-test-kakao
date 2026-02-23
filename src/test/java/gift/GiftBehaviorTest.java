package gift;

import gift.model.OptionRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftBehaviorTest {

    @LocalServerPort
    int port;

    @Autowired
    private OptionRepository optionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * Behavior 1: 선물하기 성공 시 옵션 재고가 감소한다
     *
     * Given: 카테고리, 상품, 옵션(수량=10), 보내는 회원, 받는 회원이 존재
     * When:  POST /api/gifts + Header Member-Id + Body { optionId, quantity: 3, receiverId, message }
     * Then:  HTTP 200 OK / 옵션 수량이 10→7로 감소
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
    void 선물하기_성공_시_옵션_재고가_감소한다() {
        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", 3,
                "receiverId", 2,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        // Then — DB 상태 변화 검증 (조회 API 미노출이므로 Repository 직접 조회)
        var updatedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
    }

    /**
     * Behavior 2: 재고 부족 시 선물하기가 거부되고 재고가 유지된다
     *
     * Given: 카테고리, 상품, 옵션(수량=2), 보내는 회원, 받는 회원이 존재
     * When:  POST /api/gifts + Body { optionId, quantity: 5, ... } (재고 초과)
     * Then:  HTTP 500 / 옵션 수량이 2로 유지 (변화 없음)
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup-low-stock.sql"})
    void 재고_부족_시_선물하기가_거부되고_재고가_유지된다() {
        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 1,
                "quantity", 5,
                "receiverId", 2,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);

        // Then — DB 상태 변화 검증 (재고 불변 확인 — 트랜잭션 롤백 보장)
        var unchangedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(2);
    }

    /**
     * Behavior 3: 존재하지 않는 옵션으로 선물하면 실패한다
     *
     * Given: 테이블이 비어 있는 상태 (옵션 ID 9999는 존재하지 않음)
     * When:  POST /api/gifts + Body { optionId: 9999, ... }
     * Then:  HTTP 500
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 1)
            .body(Map.of(
                "optionId", 9999,
                "quantity", 1,
                "receiverId", 2,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);
    }

    /**
     * Behavior 7: 보내는 회원 미존재 시 선물하기가 실패하고 재고가 롤백된다
     *
     * 코드 실행 순서: option.decrease() → giftDelivery.deliver() (회원 조회)
     * 존재하지 않는 회원 ID로 선물 시, decrease() 이후 deliver()에서 실패하면
     * 트랜잭션 롤백으로 재고가 원복되어야 한다.
     *
     * Given: 카테고리, 상품, 옵션(수량=10), 받는 회원이 존재 / 보내는 회원(ID=9999) 미존재
     * When:  POST /api/gifts + Header Member-Id: 9999 + Body { optionId, quantity: 3, ... }
     * Then:  HTTP 500 / 옵션 수량이 10으로 유지 (트랜잭션 원자성 보장)
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
    void 보내는_회원이_존재하지_않으면_선물이_실패하고_재고가_롤백된다() {
        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", 9999)
            .body(Map.of(
                "optionId", 1,
                "quantity", 3,
                "receiverId", 2,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(500);

        // Then — DB 상태 변화 검증 (재고 원복 확인 — 트랜잭션 롤백 보장)
        var unchangedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(10);
    }
}
