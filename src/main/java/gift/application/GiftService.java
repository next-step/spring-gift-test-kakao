package gift.application;

import gift.application.request.GiveGiftRequest;
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

    public GiftService(
        final OptionRepository optionRepository,
        final GiftDelivery giftDelivery
    ) {
        this.optionRepository = optionRepository;
        this.giftDelivery = giftDelivery;
    }

    public void give(final GiveGiftRequest request, final Long memberId) {

        Long optionId = request.optionId();

        final Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new OptionNotFoundException(optionId));

        option.decrease(request.quantity());

        final Gift gift = new Gift(
            memberId,
                request.receiverId(),
            option,
                request.quantity(),
                request.message()
        );

        giftDelivery.deliver(gift);
    }
}
