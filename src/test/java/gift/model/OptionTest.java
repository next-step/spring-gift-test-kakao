package gift.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Option 도메인 단위 테스트
 *
 * 검증하는 핵심 행위:
 * 1. 재고 감소 - 정상 케이스
 * 2. 재고 감소 - 경계값 (stock == quantity)
 * 3. 재고 감소 - 재고 부족 시 예외
 * 4. 재고 감소 - 재고 0에서 감소 시도
 * 5. 재고 감소 - 수량 0 입력
 * 6. 재고 감소 - 최소 단위 (수량 1)
 * 7. 재고 감소 - 음수 입력 (버그 문서화)
 */
@DisplayName("Option 도메인")
class OptionTest {

    private Option createOption(int stock) {
        return new Option("테스트 옵션", stock, null);
    }

    @Nested
    @DisplayName("decrease(): 재고 감소")
    class Decrease {

        @Test
        @DisplayName("재고보다 적은 수량을 감소시키면 재고가 줄어든다")
        void decreasesQuantityByGivenAmount() {
            // given
            Option option = createOption(10);

            // when
            option.decrease(3);

            // then
            assertThat(option.getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("재고와 동일한 수량을 감소시키면 재고가 정확히 0이 된다")
        void decreasesToZeroWhenQuantityEqualsStock() {
            // given
            Option option = createOption(5);

            // when
            option.decrease(5);

            // then
            assertThat(option.getQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("재고보다 많은 수량을 감소시키면 IllegalStateException이 발생한다")
        void throwsIllegalStateExceptionWhenQuantityExceedsStock() {
            // given
            Option option = createOption(3);

            // when / then
            assertThatThrownBy(() -> option.decrease(4))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("재고가 0일 때 감소를 시도하면 IllegalStateException이 발생한다")
        void throwsIllegalStateExceptionWhenStockIsAlreadyZero() {
            // given
            Option option = createOption(0);

            // when / then
            assertThatThrownBy(() -> option.decrease(1))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("수량 0으로 감소하면 재고가 변하지 않는다")
        void doesNotChangeStockWhenQuantityIsZero() {
            // given
            Option option = createOption(10);

            // when
            option.decrease(0);

            // then
            assertThat(option.getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("수량 1로 감소하면 재고가 1 줄어든다 (최소 단위)")
        void decreasesByOneWhenQuantityIsOne() {
            // given
            Option option = createOption(10);

            // when
            option.decrease(1);

            // then
            assertThat(option.getQuantity()).isEqualTo(9);
        }

        @Test
        @DisplayName("음수 수량으로 감소하면 재고가 증가하는 버그가 있다")
        void negativeQuantityIncreasesStock_BUG() {
            // given
            Option option = createOption(10);

            // when
            // 버그: decrease(-5)는 this.quantity -= (-5) = this.quantity += 5
            option.decrease(-5);

            // then
            // 버그 문서화: 음수 입력에 대한 방어 로직이 없어 재고가 증가함
            // 올바른 동작: IllegalArgumentException을 던져야 함
            assertThat(option.getQuantity()).isEqualTo(15); // 10 + 5 = 15 (버그!)
        }
    }
}
