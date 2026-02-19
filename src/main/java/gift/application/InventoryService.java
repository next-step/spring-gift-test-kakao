package gift.application;

import gift.model.Option;
import gift.model.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 관리 서비스 (리팩터링 후 도입)
 *
 * 책임: 옵션 재고 감소 로직 관리
 * - Before: GiftService가 직접 option.decrease() 호출
 * - After: InventoryService가 재고 관리 책임 담당
 *
 * 리팩터링 목적:
 * - 단일 책임 원칙 (SRP): 재고 관리 로직 분리
 * - 재사용성: 다른 서비스에서도 재고 감소 로직 사용 가능
 */
@Service
@Transactional
public class InventoryService {
    private final OptionRepository optionRepository;

    public InventoryService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * 옵션 재고 감소
     *
     * @param optionId 옵션 ID
     * @param quantity 감소할 수량
     * @throws IllegalStateException 재고 부족 시
     */
    public void decreaseStock(Long optionId, int quantity) {
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다."));

        option.decrease(quantity);  // 재고 감소 (여전히 Option 객체가 담당)
    }
}