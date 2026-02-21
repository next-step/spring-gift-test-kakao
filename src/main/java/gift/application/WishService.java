package gift.application;

import gift.application.request.CreateWishRequest;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import gift.model.Wish;
import gift.model.WishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class WishService {
    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public WishService(
        final WishRepository wishRepository,
        final MemberRepository memberRepository,
        final ProductRepository productRepository
    ) {
        this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    public Wish create(final Long memberId, final CreateWishRequest request) {

        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        Long productId = request.productId();
        final Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return wishRepository.save(new Wish(member, product));
    }
}
