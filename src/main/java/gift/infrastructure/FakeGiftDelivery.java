package gift.infrastructure;

import gift.error.CommonErrorCode;
import gift.error.CommonException;
import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.Product;
import org.springframework.stereotype.Component;

@Component
class FakeGiftDelivery implements GiftDelivery {
    private final MemberRepository memberRepository;

    public FakeGiftDelivery(final MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void deliver(final Gift gift) {
        final Member member = memberRepository.findById(gift.getFrom())
                .orElseThrow(() -> new CommonException(CommonErrorCode.MEMBER_NOT_FOUND));
        final Option option = gift.getOption();
        final Product product = option.getProduct();
        System.out.println(member.getName() + product.getName() + option.getName() + option.getQuantity());
    }
}
