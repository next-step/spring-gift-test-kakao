package gift.application;

import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.Option;
import gift.model.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class GiftService {
    private final OptionRepository optionRepository;
    private final GiftDelivery giftDelivery;
    private final InventoryService inventoryService;  // ← 새로 추가

    public GiftService(
            final OptionRepository optionRepository,
            final GiftDelivery giftDelivery, InventoryService inventoryService
    ) {
        this.optionRepository = optionRepository;
        this.giftDelivery = giftDelivery;
        this.inventoryService = inventoryService;
    }

    public void give(final GiveGiftRequest request, final Long memberId) {
        inventoryService.decreaseStock(memberId, request.getQuantity());
        
        final Option option = optionRepository.findById(request.getOptionId()).orElseThrow();
        final Gift gift = new Gift(
            memberId,
            request.getReceiverId(),
            option,
            request.getQuantity(),
            request.getMessage()
        );
        giftDelivery.deliver(gift);
    }
}
