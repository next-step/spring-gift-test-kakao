package gift;

import gift.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

/**
 * 선물하기 시스템 인수 테스트
 *
 * 검증하는 10가지 핵심 행위:
 * 1. 선물하기 - 재고 감소
 * 2. 선물하기 - 누적 재고 감소
 * 3. 선물하기 - 재고 부족 시 실패
 * 4. 선물하기 - 재고 소진 후 실패
 * 5. 선물하기 - 존재하지 않는 옵션으로 실패
 * 6. 선물하기 - 재고와 정확히 같은 수량 (경계값)
 * 7. 선물하기 - 수량 0으로 선물 시도
 * 8. 선물하기 - 음수 수량으로 선물 시도 (버그 문서화)
 * 9. 선물하기 - 존재하지 않는 발신자로 실패
 * 10. 선물하기 - 실패 후 정상 선물로 불변성 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class GiftAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    private String baseUrl;
    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
    }

    @Nested
    @DisplayName("행위 1: 선물하기 시 재고 감소")
    class GiftDecreasesStock {

        @Test
        @DisplayName("선물하면 옵션의 재고가 요청한 수량만큼 감소한다")
        void decreasesStockByRequestedQuantity() {
            // given
            Option option = createOptionWithStock(10);

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), 3);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("행위 2: 여러 번 선물 시 누적 재고 감소")
    class MultipleGiftsDecreaseStockCumulatively {

        @Test
        @DisplayName("동일 옵션으로 여러 번 선물하면 재고가 누적 감소한다")
        void decreasesStockCumulatively() {
            // given
            Option option = createOptionWithStock(20);

            // when
            ResponseEntity<Void> firstResponse = sendGift(option.getId(), 5);
            ResponseEntity<Void> secondResponse = sendGift(option.getId(), 7);

            // then
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("행위 3: 재고 부족 시 선물 실패")
    class GiftFailsWhenInsufficientStock {

        @Test
        @DisplayName("재고보다 많은 수량을 선물하면 실패하고 재고는 유지된다")
        void failsAndKeepsStockUnchanged() {
            // given
            Option option = createOptionWithStock(5);

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), 10);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(getStock(option.getId())).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("행위 4: 재고 소진 후 선물 실패")
    class GiftFailsWhenStockExhausted {

        @Test
        @DisplayName("재고가 0이 된 후 추가 선물을 시도하면 실패한다")
        void failsWhenStockIsZero() {
            // given
            Option option = createOptionWithStock(3);
            sendGift(option.getId(), 3);  // 재고 소진

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), 1);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(getStock(option.getId())).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("행위 5: 존재하지 않는 옵션으로 선물 실패")
    class GiftFailsWithInvalidOption {

        @Test
        @DisplayName("존재하지 않는 옵션 ID로 선물하면 실패한다")
        void failsWithNonExistentOptionId() {
            // given
            Long invalidOptionId = 99999L;

            // when
            ResponseEntity<Void> response = sendGift(invalidOptionId, 1);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("행위 6: 재고와 정확히 같은 수량 선물 (경계값)")
    class GiftSucceedsWhenQuantityEqualsStock {

        @Test
        @DisplayName("재고와 동일한 수량을 선물하면 성공하고 재고가 0이 된다")
        void succeedsAndStockBecomesZero() {
            // given
            Option option = createOptionWithStock(5);

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), 5);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("행위 7: 수량 0으로 선물 시도")
    class GiftWithZeroQuantity {

        @Test
        @DisplayName("수량 0으로 선물하면 재고가 변하지 않고 성공한다")
        void succeedsWithZeroQuantityAndStockUnchanged() {
            // given
            Option option = createOptionWithStock(10);

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), 0);

            // then
            // 현재 구현: 수량 0은 허용됨 (decrease(0)은 재고 변경 없음)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("행위 8: 음수 수량으로 선물 시도 (버그 검출)")
    class GiftWithNegativeQuantity {

        @Test
        @DisplayName("음수 수량으로 선물하면 재고가 증가하는 버그가 있다")
        void negativeQuantityIncreasesStock_BUG() {
            // given
            Option option = createOptionWithStock(10);

            // when
            ResponseEntity<Void> response = sendGift(option.getId(), -5);

            // then
            // 버그 문서화: 현재 구현에서 음수 수량은 재고를 증가시킨다
            // decrease(-5) -> this.quantity -= (-5) -> this.quantity += 5
            // 이 테스트는 버그를 발견하고 문서화하는 역할
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(15); // 10 + 5 = 15 (버그!)
        }
    }

    @Nested
    @DisplayName("행위 9: 존재하지 않는 발신자로 선물 실패")
    class GiftFailsWithInvalidSender {

        @Test
        @DisplayName("존재하지 않는 Member-Id로 선물하면 실패한다")
        void failsWithNonExistentSenderId() {
            // given
            Option option = createOptionWithStock(10);
            Long invalidSenderId = 99999L;

            // when
            ResponseEntity<Void> response = sendGiftWithSender(option.getId(), 1, invalidSenderId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            // 재고에 부작용 없음을 검증
            assertThat(getStock(option.getId())).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("행위 10: 실패 후 정상 선물로 재고 불변성 검증")
    class GiftFailureThenSuccessVerifiesStockIntegrity {

        @Test
        @DisplayName("재고 부족으로 실패 후 정상 선물하면 재고가 올바르게 감소한다")
        void failureThenSuccessVerifiesNoPartialEffect() {
            // given
            Option option = createOptionWithStock(5);

            // when - 실패 요청
            ResponseEntity<Void> failedResponse = sendGift(option.getId(), 10);
            assertThat(failedResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            // when - 성공 요청
            ResponseEntity<Void> successResponse = sendGift(option.getId(), 3);

            // then
            assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getStock(option.getId())).isEqualTo(2); // 5 - 3 = 2
        }
    }

    // === Test Fixtures ===

    private Option createOptionWithStock(int stock) {
        Category category = categoryRepository.save(new Category("테스트 카테고리"));
        Product product = productRepository.save(
                new Product("테스트 상품", 10000, "http://test.jpg", category)
        );
        return optionRepository.save(new Option("테스트 옵션", stock, product));
    }

    private int getStock(Long optionId) {
        return optionRepository.findById(optionId)
                .orElseThrow()
                .getQuantity();
    }

    // === API Helpers ===

    private ResponseEntity<Void> sendGift(Long optionId, int quantity) {
        return sendGiftWithSender(optionId, quantity, sender.getId());
    }

    private ResponseEntity<Void> sendGiftWithSender(Long optionId, int quantity, Long senderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Member-Id", String.valueOf(senderId));

        Map<String, Object> request = Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", receiver.getId(),
                "message", "선물입니다"
        );

        return restTemplate.exchange(
                baseUrl + "/gifts",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Void.class
        );
    }
}
