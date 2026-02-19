package gift.application;

import gift.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {
    @Mock
    private OptionRepository optionRepository;

    @Mock
    private GiftDelivery giftDelivery;

    @InjectMocks
    private GiftService giftService;

    @Test
    @DisplayName("선물을 보내면 재고가 감소한다 (구현 검증)")
    void 선물을_보내면_재고가_감소한다() {
        // Given
        Category category = new Category(1L, "전자기기");
        Product product = new Product(1L, "아이폰", 1000000, "http://image.png", category);
        Option option = spy(new Option(1L, "128GB", 10, product));

        when(optionRepository.findById(1L)).thenReturn(Optional.of(option));

        GiveGiftRequest request = new GiveGiftRequest(1L, 1, 2L, "생일 축하");

        // When
        giftService.give(request, 1L);

        // Then
        verify(option, times(1)).decrease(1);
        verify(giftDelivery, times(1)).deliver(any(Gift.class));
    }
}