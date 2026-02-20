package gift.application;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class GiftServiceTest {

    @Autowired
    private GiftService giftService;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private GiftDelivery giftDelivery;

    private Category category;
    private Product product;
    private Option option;

    @BeforeEach
    void setUp() {
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("테스트 카테고리"));
        product = productRepository.save(new Product("테스트 상품", 10000, "http://image.url", category));
        option = optionRepository.save(new Option("기본 옵션", 10, product));
    }

    @Nested
    @DisplayName("give: 선물 전송")
    class Give {

        @Test
        void 선물_전송_성공_시_재고가_감소한다() throws Exception {
            // given
            doNothing().when(giftDelivery).deliver(any(Gift.class));
            GiveGiftRequest request = createRequest(option.getId(), 3, 2L, "축하해");

            // when
            giftService.give(request, 1L);

            // then
            Option updated = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(updated.getQuantity()).isEqualTo(7);
        }

        @Test
        void 선물_전달_실패_시_재고가_롤백된다() throws Exception {
            // given
            doThrow(new RuntimeException("전달 실패"))
                    .when(giftDelivery).deliver(any(Gift.class));
            GiveGiftRequest request = createRequest(option.getId(), 3, 2L, "축하해");
            int initialQuantity = option.getQuantity();

            // when & then
            assertThatThrownBy(() -> giftService.give(request, 1L))
                    .isInstanceOf(RuntimeException.class);

            // 트랜잭션 롤백 확인
            Option updated = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(updated.getQuantity()).isEqualTo(initialQuantity);
        }

        @Test
        void 존재하지_않는_옵션이면_예외가_발생한다() throws Exception {
            // given
            GiveGiftRequest request = createRequest(999L, 1, 2L, "축하해");

            // when & then
            assertThatThrownBy(() -> giftService.give(request, 1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    private GiveGiftRequest createRequest(Long optionId, int quantity, Long receiverId, String message) throws Exception {
        GiveGiftRequest request = new GiveGiftRequest();
        setField(request, "optionId", optionId);
        setField(request, "quantity", quantity);
        setField(request, "receiverId", receiverId);
        setField(request, "message", message);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
