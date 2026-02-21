package gift.infrastructure;

import gift.application.MemberNotFoundException;
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
        Long senderId = gift.from();
        Long receiverId = gift.to();

        final Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new MemberNotFoundException(senderId));
        final Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new MemberNotFoundException(receiverId));

        final Option option = gift.option();
        final Product product = option.getProduct();

        System.out.printf("""
                        선물 송신자 \t: [%d] - %s
                        선물 수신자 \t: [%d] - %s
                        상품 이름 \t: %s
                        옵션 이름 \t: %s
                        옵션 재고 \t: %d
                        """,
                senderId, sender.getName(),
                receiverId, receiver.getName(),
                product.getName(),
                option.getName(), option.getQuantity()
        );
    }
}
