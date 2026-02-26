package gift.fixture;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;

public class GiftFixture {
    private final Long senderId;
    private final Long receiverId;
    private final Long optionId;
    private final int initialQuantity;

    private GiftFixture(final Long senderId, final Long receiverId, final Long optionId, final int initialQuantity) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.optionId = optionId;
        this.initialQuantity = initialQuantity;
    }

    public static Builder builder(
            final MemberRepository memberRepository,
            final CategoryRepository categoryRepository,
            final ProductRepository productRepository,
            final OptionRepository optionRepository
    ) {
        return new Builder(memberRepository, categoryRepository, productRepository, optionRepository);
    }

    public Long senderId() {
        return senderId;
    }

    public Long receiverId() {
        return receiverId;
    }

    public Long optionId() {
        return optionId;
    }

    public int initialQuantity() {
        return initialQuantity;
    }

    public static class Builder {
        private final MemberRepository memberRepository;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final OptionRepository optionRepository;

        private String senderName = "보내는사람";
        private String senderEmail = "sender@example.com";
        private String receiverName = "받는사람";
        private String receiverEmail = "receiver@example.com";
        private String categoryName = "테스트카테고리";
        private String productName = "테스트상품";
        private int productPrice = 1000;
        private String productImageUrl = "https://example.com/image.png";
        private String optionName = "테스트옵션";
        private int optionQuantity = 1;

        private Builder(
                final MemberRepository memberRepository,
                final CategoryRepository categoryRepository,
                final ProductRepository productRepository,
                final OptionRepository optionRepository
        ) {
            this.memberRepository = memberRepository;
            this.categoryRepository = categoryRepository;
            this.productRepository = productRepository;
            this.optionRepository = optionRepository;
        }

        public Builder sender(final String name, final String email) {
            this.senderName = name;
            this.senderEmail = email;
            return this;
        }

        public Builder receiver(final String name, final String email) {
            this.receiverName = name;
            this.receiverEmail = email;
            return this;
        }

        public Builder category(final String name) {
            this.categoryName = name;
            return this;
        }

        public Builder product(final String name, final int price, final String imageUrl) {
            this.productName = name;
            this.productPrice = price;
            this.productImageUrl = imageUrl;
            return this;
        }

        public Builder option(final String name, final int quantity) {
            this.optionName = name;
            this.optionQuantity = quantity;
            return this;
        }

        public GiftFixture build() {
            Member sender = memberRepository.save(new Member(senderName, senderEmail));
            Member receiver = memberRepository.save(new Member(receiverName, receiverEmail));
            Category category = categoryRepository.save(new Category(categoryName));
            Product product = productRepository.save(new Product(productName, productPrice, productImageUrl, category));
            Option option = optionRepository.save(new Option(optionName, optionQuantity, product));

            return new GiftFixture(sender.getId(), receiver.getId(), option.getId(), option.getQuantity());
        }
    }
}
