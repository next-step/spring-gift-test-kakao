package gift;

import gift.model.OptionRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GiftBehaviorTest extends BaseBehaviorTest {

    @Autowired
    private OptionRepository optionRepository;

    /**
     * Behavior 1: 선물하기 성공 시 옵션 재고가 감소한다
     *
     * Given: 카테고리, 상품, 옵션(수량=10), 보내는 회원, 받는 회원이 존재
     * When:  POST /api/gifts + Header Member-Id + Body { optionId, quantity: 3, receiverId, message }
     * Then:  HTTP 200 OK / 옵션 수량이 10→7로 감소, 옵션의 다른 속성은 불변
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
    void 재고가_충분할_때_선물하기를_하면_옵션_수량이_감소한다() {
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

        // Then — DB 상태 변화 검증: 수량만 감소하고 나머지 속성은 불변
        var updatedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(7);
        assertThat(updatedOption.getName()).isEqualTo("테스트옵션");
        assertThat(updatedOption.getProduct().getId()).isEqualTo(1L);
    }

    /**
     * Behavior 2: 재고 부족 시 선물하기가 거부되고 재고가 유지된다
     *
     * Given: 카테고리, 상품, 옵션(수량=2), 보내는 회원, 받는 회원이 존재
     * When:  POST /api/gifts + Body { optionId, quantity: 5, ... } (재고 초과)
     * Then:  HTTP 500 / 옵션 수량이 2로 유지, 옵션의 다른 속성도 불변
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup-low-stock.sql"})
    void 재고가_부족할_때_선물하기를_하면_거부되고_재고가_유지된다() {
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

        // Then — DB 상태 불변 검증: 재고 및 옵션 전체 속성 유지
        var unchangedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(2);
        assertThat(unchangedOption.getName()).isEqualTo("저재고옵션");
        assertThat(unchangedOption.getProduct().getId()).isEqualTo(1L);
    }

    /**
     * Behavior 3: 존재하지 않는 옵션으로 선물하면 실패한다
     *
     * Given: 테이블이 비어 있는 상태 (옵션 ID 9999는 존재하지 않음)
     * When:  POST /api/gifts + Body { optionId: 9999, ... }
     * Then:  HTTP 500 / DB에 옵션이 생성되지 않음 (빈 상태 유지)
     */
    @Test
    @Sql("/sql/cleanup.sql")
    void 옵션이_존재하지_않을_때_선물하기를_하면_실패한다() {
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

        // Then — DB 상태 불변 검증: 옵션 테이블에 아무것도 생성되지 않음
        assertThat(optionRepository.findAll()).isEmpty();
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
     * Then:  HTTP 500 / 옵션 수량이 10으로 유지, 옵션의 다른 속성도 불변 (트랜잭션 원자성 보장)
     */
    @Test
    @Sql({"/sql/cleanup.sql", "/sql/gift-setup.sql"})
    void 보내는_회원이_존재하지_않을_때_선물하기를_하면_실패하고_재고가_롤백된다() {
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

        // Then — DB 상태 불변 검증: 트랜잭션 롤백으로 재고 및 옵션 전체 속성 원복
        var unchangedOption = optionRepository.findById(1L).orElseThrow();
        assertThat(unchangedOption.getQuantity()).isEqualTo(10);
        assertThat(unchangedOption.getName()).isEqualTo("테스트옵션");
        assertThat(unchangedOption.getProduct().getId()).isEqualTo(1L);
    }
}

