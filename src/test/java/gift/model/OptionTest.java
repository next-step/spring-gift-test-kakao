package gift.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    @Test
    @DisplayName("충분한 재고에서 수량을 차감하면 재고가 줄어든다")
    void decrease_sufficientStock_reducesQuantity() {
        // Arrange
        final Option option = new Option("기본 옵션", 100, null);

        // Act
        option.decrease(30);

        // Assert
        assertThat(option.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
    void decrease_insufficientStock_throwsException() {
        // Arrange
        final Option option = new Option("기본 옵션", 5, null);

        // Act & Assert
        assertThatThrownBy(() -> option.decrease(10))
                .isInstanceOf(IllegalStateException.class);
    }
}
