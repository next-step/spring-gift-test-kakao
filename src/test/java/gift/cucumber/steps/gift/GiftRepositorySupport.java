package gift.cucumber.steps.gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class GiftRepositorySupport {
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public GiftRepositorySupport(
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

    public Member seedMember(final String name, final String email) {
        return memberRepository.save(new Member(name, email));
    }

    public Product seedProduct(final String name, final Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        return productRepository.save(new Product(name, 1_500_000, "https://example.com/image.png", category));
    }

    public Option seedOption(final String name, final int quantity, final Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return optionRepository.save(new Option(name, quantity, product));
    }

    public void assertOptionQuantity(final Long optionId, final int expectedQuantity) {
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
